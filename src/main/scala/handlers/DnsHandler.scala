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
import datastructures.DNSCache
import enums.RecordType
import models.Host
import payload.Header
import models.AddressHost
import records.AbstractRecord
import models.ExtendedDomain
import payload.RRData
import datastructures.DomainNotFoundException
import models.HostNotFoundException
import enums.ResponseCode
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener
import models.CnameHost
import scala.collection.mutable
import datastructures.DomainNotFoundException
import models.HostNotFoundException
import datastructures.DomainNotFoundException
import payload.Name
import records.CNAME
import scala.annotation.tailrec

class DnsHandler extends SimpleChannelUpstreamHandler {

  val logger = LoggerFactory.getLogger("app")

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case message: Message => {
        logger.info(message.toString)
        logger.info("Request bytes: " + message.toByteArray.toList.toString)
        val response = try {
          val answers = distinctAliases(message.query.map { query =>
            val qname = query.qname.filter(_.length > 0).map(new String(_, "UTF-8"))
            val domain = DNSCache.getDomain(query.qtype, qname)

            val records = {
              val rdata = DnsResponseBuilder.hostToRecords(qname, query.qtype, query.qclass)
              if (!rdata.isEmpty) rdata else DnsResponseBuilder.wildcardsToRecords(domain, qname, query.qtype, query.qclass)
            }

            // @TODO: Implement additional where appropriate

            // @TODO: Return SOA when host not found

            records.map {
              case (name, record) =>
                new RRData(
                  (((name).split("""\.""") :+ "").map(_.getBytes)).toList,
                  RecordType.withName(record.description).id,
                  query.qclass,
                  domain.ttl,
                  record.toByteArray.length,
                  record)
            }
          }.flatten)

          if (!answers.isEmpty) {
            val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
              message.header.recursionDesired, false, 0, ResponseCode.OK.id, message.header.questionCount, answers.length, 0, 0)
            Message(header, message.query, answers, message.authority, message.additional)
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
        val responseBytes = response.toByteArray
        logger.debug("Response length: " + responseBytes.length.toString)
        logger.debug("Response bytes: " + responseBytes.toList.toString)
        e.getChannel.write(ChannelBuffers.copiedBuffer(responseBytes), e.getRemoteAddress)
      }
      case _ => {
        logger.error("Unsupported message type")
      }
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
}
