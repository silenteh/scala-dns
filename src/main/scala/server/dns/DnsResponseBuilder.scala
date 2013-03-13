/*******************************************************************************
 * Copyright 2012 silenteh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package server.dns

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
import scala.Array.canBuildFrom
import scala.annotation.tailrec
import configs.ConfigService
import scala.collection.JavaConversions._
import datastructures.DNSAuthoritativeSection
import records.SOA
import models.SoaHost

object DnsResponseBuilder {

  val logger = LoggerFactory.getLogger("app")

  def apply(message: Message, sourceIP: String, maxLength: Int = -1, compress: Boolean = true) = {
    val response = try {
      val responseParts = message.query.map { query =>
        val qname = query.qname.filter(_.length > 0).map(new String(_, "UTF-8"))
        //val domain = DNSCache.getDomain(query.qtype, qname)
        val domain = DNSAuthoritativeSection.getDomain(query.qtype, qname)
        
        val records =
          // Zone file transfer
          if(query.qtype == RecordType.AXFR.id) {
            val allowedIps = ConfigService.config.getStringList("zoneTransferAllowedIps").toList
            if(maxLength != -1) throw new DomainNotFoundException
            else if(!allowedIps.contains(sourceIP.substring(1, sourceIP.lastIndexOf(":")))) throw new DomainNotFoundException
            else DnsLookupService.zoneToRecords(qname, query.qclass)
            
          // Standard DNS response
          } else {
            val rdata = DnsLookupService.hostToRecords(qname, query.qtype, query.qclass)
            if (!rdata.isEmpty) rdata
            else DnsLookupService.ancestorToRecords(domain, qname, query.qtype, query.qclass, true)
          }
        
        // @TODO: Return NS when host not found
        
        val authority =
          if (!records.isEmpty || query.qtype == RecordType.AXFR.id) List[(String, AbstractRecord)]()
          else {
            val records = DnsLookupService.hostToRecords(qname, RecordType.NS.id, query.qclass)
            if(!records.isEmpty) records
            else DnsLookupService.ancestorToRecords(domain, qname, RecordType.NS.id, query.qclass, false)
          }

        // @TODO: Implement additional where appropriate

        val additionals = {
          /*if (query.qtype != RecordType.A.id) List[(String, AbstractRecord)]()
          else */(records ++ authority).map {
            case (name, record) =>
              try {
                record match {
                  case r: MX => DnsLookupService.hostToRecordsWithDefault(bytesToName(r.record), RecordType.A.id, query.qclass)
                  case r: NS => DnsLookupService.hostToRecordsWithDefault(bytesToName(r.record), RecordType.A.id, query.qclass)
                  case r: SOA => DnsLookupService.hostToRecordsWithDefault(bytesToName(r.mname), RecordType.A.id, query.qclass) ++
                    DnsLookupService.hostToRecordsWithDefault(bytesToName(r.rname), RecordType.A.id, query.qclass)
                  case _ => List[(String, AbstractRecord)]()
                }
              } catch {
                case e: DomainNotFoundException => List[(String, AbstractRecord)]()
              }
          }.flatten
        }
          
        List(
          "answer" -> recordsToRRData(domain, query.qclass, records),
          "authority" -> recordsToRRData(domain, query.qclass, authority),
          "additional" -> recordsToRRData(domain, query.qclass, additionals))
      }.flatten.groupBy(_._1).map { case (typ, records) => (typ, records.map(_._2).flatten) }
      
      val (answers, authorities, additionals) =
        (responseParts("answer"), responseParts("authority"), responseParts("additional"))
        
      if (!answers.isEmpty) {
        val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
          message.header.recursionDesired, false, 0, ResponseCode.OK.id, message.header.questionCount, 
            answers.length, authorities.length, additionals.length)
        Message(header, message.query, answers, authorities, additionals)
      } else {
        val rcode = if(authorities.isEmpty) ResponseCode.NAME_ERROR.id else ResponseCode.OK.id
        val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
          message.header.recursionDesired, false, 0, rcode, message.header.questionCount, 0, authorities.length, additionals.length)
        Message(header, message.query, message.answers, authorities, additionals)
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

    val bytes = response.toCompressedByteArray((Array[Byte](), Map[String, Int]()))._1
    
    if (maxLength < 0 || bytes.length <= maxLength) bytes
    else {
      val headerBytes = response.header.setTruncated(true).toCompressedByteArray(Array[Byte](), Map[String, Int]())._1
      (headerBytes ++ bytes.takeRight(bytes.length - headerBytes.length)).take(maxLength)
    }
  }
  
  private def bytesToName(bytes: List[Array[Byte]]) = 
    bytes.filterNot(s => s.isEmpty || (s.length == 1 && s.head == 0)).map(new String(_, "UTF-8"))
  
  private def recordsToRRData(domain: ExtendedDomain, qclass: Int, records: List[(String, AbstractRecord)]) =
    records.map {
      case (name, record) => {
        val nameParts = name.split("""\.""")
        val nameBytes = ((nameParts :+ "").map(_.getBytes)).toList
        val ttl = if(domain.settings.size == 1) domain.settings.head.ttlToLong else ttlForNamePart(nameParts, domain)
        new RRData(nameBytes, RecordType.withName(record.description).id, qclass, ttl, record.toByteArray.length, record)
      }
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
  
  @tailrec
  def ttlForNamePart(name: Array[String], domain: ExtendedDomain): Long = 
    if(name.isEmpty) domain.ttl.toLong
    else domain.findHost(DnsLookupService.relativeHostName(name.toList, domain), RecordType.SOA.id) match {
      case Some(soa: SoaHost) => soa.ttlToLong
      case _ => ttlForNamePart(name.tail, domain)
    }
}