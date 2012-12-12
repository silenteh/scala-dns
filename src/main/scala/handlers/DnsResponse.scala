package handlers

import org.slf4j.LoggerFactory
import payload.Message
import datastructures.DNSCache
import records.AbstractRecord
import enums.RecordType
import records.MX
import records.NS
import models.ExtendedDomain
import payload.RRData
import scala.annotation.tailrec
import records.CNAME
import payload.Header
import enums.ResponseCode
import datastructures.DomainNotFoundException

object DnsResponse {

  val logger = LoggerFactory.getLogger("app")

  def apply(message: Message, maxLength: Int = -1, compress: Boolean = true) = {
    val response = try {
      val responseParts = message.query.map { query =>
        val qname = query.qname.filter(_.length > 0).map(new String(_, "UTF-8"))
        val domain = DNSCache.getDomain(query.qtype, qname)

        val records = {
          val rdata = DnsLookupService.hostToRecords(qname, query.qtype, query.qclass)
          if (!rdata.isEmpty) rdata
          else DnsLookupService.ancestorToRecords(domain, qname, query.qtype, query.qclass, true)
        }

        // @TODO: Return NS when host not found

        val authority =
          if (!records.isEmpty) List[(String, AbstractRecord)]()
          else DnsLookupService.ancestorToRecords(domain, qname, RecordType.NS.id, query.qclass, false)

        // @TODO: Implement additional where appropriate

        val additionals =
          if (query.qtype != RecordType.A.id) List[(String, AbstractRecord)]()
          else (records ++ authority).map {
            case (name, record) =>
              record match {
                case r: MX =>
                  DnsLookupService.hostToRecords(r.record.map(new String(_, "UTF-8")), RecordType.A.id, query.qclass)
                case r: NS =>
                  DnsLookupService.hostToRecords(r.record.map(new String(_, "UTF-8")), RecordType.A.id, query.qclass)
                case _ =>
                  List[(String, AbstractRecord)]()
              }
          }.flatten

        List(
          "answer" -> recordsToRRData(domain, query.qclass, records),
          "authority" -> recordsToRRData(domain, query.qclass, authority),
          "additional" -> recordsToRRData(domain, query.qclass, additionals))
      }.flatten.groupBy(_._1).map { case (typ, records) => (typ, records.map(_._2).flatten) }

      val (answers, authorities, additionals) =
        (responseParts("answer"), responseParts("authority"), responseParts("additional"))

      if (!answers.isEmpty) {
        val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
          message.header.recursionDesired, false, 0, ResponseCode.OK.id, message.header.questionCount, answers.length, 0, 0)
        Message(header, message.query, answers, authorities, additionals)
      } else {
        val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
          message.header.recursionDesired, false, 0, ResponseCode.NAME_ERROR.id, message.header.questionCount, 0, 0, 0)
        Message(header, message.query, message.answers, message.authority, message.additional)
      }
    } catch {
      case ex: DomainNotFoundException => {
        val header = Header(message.header.id, true, message.header.opcode, false, message.header.truncated,
          message.header.recursionDesired, false, 0, ResponseCode.REFUSED.id, message.header.questionCount, 0, 0, 0)
        Message(header, message.query, message.answers, message.authority, message.additional)
      }
      case ex: Exception => {
        logger.error(ex.getClass.getName + "\n" + ex.getStackTraceString)
        val header = Header(message.header.id, true, message.header.opcode, false, message.header.truncated,
          message.header.recursionDesired, false, 0, ResponseCode.SERVER_FAILURE.id, message.header.questionCount, 0, 0, 0)
        Message(header, message.query, message.answers, message.authority, message.additional)
      }
    }
    /*val responseBytes = response.toByteArray
        logger.debug("Response length: " + responseBytes.length.toString)
        logger.debug("Response bytes: " + responseBytes.toList.toString)*/
    val bytes = response.toCompressedByteArray((Array[Byte](), Map[String, Int]()))._1
    if (maxLength < 0 || bytes.length <= maxLength) bytes
    else {
      val headerBytes = response.header.setTruncated(true).toCompressedByteArray(Array[Byte](), Map[String, Int]())._1
      (headerBytes ++ bytes.takeRight(bytes.length - headerBytes.length)).take(maxLength)
    }
  }

  private def recordsToRRData(domain: ExtendedDomain, qclass: Int, records: List[(String, AbstractRecord)]) =
    records.map {
      case (name, record) =>
        new RRData(
          (((name).split("""\.""") :+ "").map(_.getBytes)).toList,
          RecordType.withName(record.description).id,
          qclass,
          domain.ttl,
          record.toByteArray.length,
          record)
    }

  @tailrec
  private def distinctAliases(records: Array[RRData], results: Array[RRData] = Array()): Array[RRData] =
    if (records.isEmpty) results
    else records.head match {
      case RRData(name, _, _, _, _, record: CNAME) =>
        if (results.exists(_.name.toArray.deep == name.toArray.deep)) distinctAliases(records.tail, results)
        else distinctAliases(records.tail, results :+ records.head)
      case _ => distinctAliases(records.tail, results :+ records.head)
    }
}