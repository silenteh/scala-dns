/**
 * *****************************************************************************
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
 * ****************************************************************************
 */
package handlers
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.slf4j.LoggerFactory
import payload.Message
import payload.Name
import payload.Header
import payload.RRData
import datastructures.DNSCache
import datastructures.DomainNotFoundException
import enums.RecordType
import enums.ResponseCode
import models.Host
import models.AddressHost
import models.ExtendedDomain
import models.HostNotFoundException
import models.CnameHost
import records._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener

import scala.collection.mutable

import scala.annotation.tailrec
import configs.ConfigService

class DnsHandler extends SimpleChannelUpstreamHandler {

  val logger = LoggerFactory.getLogger("app")
  val UdpResponseMaxSize = ConfigService.config.getInt("udpResponseMaxSize")

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case message: Message => {
        logger.info(message.toString)
        logger.info("Request bytes: " + message.toByteArray.toList.toString)
        val response = try {
          val responseParts = message.query.map { query =>
            val qname = query.qname.filter(_.length > 0).map(new String(_, "UTF-8"))
            val domain = DNSCache.getDomain(query.qtype, qname)
            
            val records = {
              val rdata = DnsResponseBuilder.hostToRecords(qname, query.qtype, query.qclass)
              if (!rdata.isEmpty) rdata 
              else DnsResponseBuilder.ancestorToRecords(domain, qname, query.qtype, query.qclass, true)
            }
            
            // @TODO: Return NS when host not found
            
            val authority = 
              if(!records.isEmpty) List[(String, AbstractRecord)]()
              else DnsResponseBuilder.ancestorToRecords(domain, qname, RecordType.NS.id, query.qclass, false)
            
            // @TODO: Implement additional where appropriate
            
            val additionals = 
              if(query.qtype != RecordType.A.id) List[(String, AbstractRecord)]()
              else (records ++ authority).map {case(name, record) =>
                record match {
                  case r: MX => 
                    DnsResponseBuilder.hostToRecords(r.record.map(new String(_, "UTF-8")), RecordType.A.id, query.qclass)
                  case r: NS => 
                    DnsResponseBuilder.hostToRecords(r.record.map(new String(_, "UTF-8")), RecordType.A.id, query.qclass)
                  case _ => 
                    List[(String, AbstractRecord)]()
                }
              }.flatten
              
            List(
              "answer" -> recordsToRRData(domain, query.qclass, records),
              "authority" -> recordsToRRData(domain, query.qclass, authority),
              "additional" -> recordsToRRData(domain, query.qclass, additionals)
            )
          }.flatten.groupBy(_._1).map { case(typ, records) => (typ, records.map(_._2).flatten) }
          
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
        val compressedResponseBytes = {
          val bytes = response.toCompressedByteArray((Array[Byte](), Map[String, Int]()))._1
          if(bytes.length <= UdpResponseMaxSize) bytes
          else {
            val headerBytes = response.header.setTruncated(true).toCompressedByteArray(Array[Byte](), Map[String, Int]())._1
            headerBytes ++ bytes.takeRight(bytes.length - headerBytes.length)
          }
        }
        
        logger.debug("Compressed response length: " + compressedResponseBytes.length.toString)
        logger.debug("Compressed response bytes: " + compressedResponseBytes.toList.toString)
        e.getChannel.write(ChannelBuffers.copiedBuffer(compressedResponseBytes), e.getRemoteAddress)
      }
      case _ => {
        logger.error("Unsupported message type")
      }
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
