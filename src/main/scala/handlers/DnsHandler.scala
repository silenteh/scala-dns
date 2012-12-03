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
          val answers = message.query.map { query =>
            val qname = query.qname.filter(_.length > 0).map(new String(_, "UTF-8"))
            val domain = DNSCache.getDomain(query.qtype, qname)

            /*val hostname = {
              val hnm = qname.take(qname.indexOfSlice(domain.nameParts)).mkString(".")
              if (hnm.length == 0 || hnm == "@") domain.fullName else hnm
            }
            
			domain.getHost(hostname, query.qtype).toRData.map { record =>
              new RRData(
                ((domain.fullName.split("""\.""") :+ "").map(_.getBytes)).toList,
                RecordType.withName(record.description).id,
                query.qclass,
                domain.ttl,
                record.toByteArray.length,
                record)
            }

            val hosts = domain.getHosts(hostname).filter(h => h.typ == RecordType(query.qtype).toString || h.typ == RecordType.CNAME.toString).map(host => host match {
              case host: CnameHost => resolveCname(host, query.qtype, query.qclass)
              case _ => host.toRData
            })

            hosts.flatten.map { record =>
              new RRData(
                ((domain.fullName.split("""\.""") :+ "").map(_.getBytes)).toList,
                RecordType.withName(record.description).id,
                query.qclass,
                domain.ttl,
                record.toByteArray.length,
                record)
            }*/

            val records = {
              val rdata = hostToRecords(qname, query.qtype, query.qclass)

              if (!rdata.isEmpty) rdata
              else wildcardsToRecords(domain, qname, query.qtype, query.qclass)
            }

            records.map { record =>
              new RRData(
                ((domain.fullName.split("""\.""") :+ "").map(_.getBytes)).toList,
                RecordType.withName(record.description).id,
                query.qclass,
                domain.ttl,
                record.toByteArray.length,
                record)
            }

          }.flatten

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
          /*case ex: HostNotFoundException => {
            val header = Header(message.header.id, true, message.header.opcode, true, message.header.truncated,
              message.header.recursionDesired, false, 0, ResponseCode.NAME_ERROR.id, message.header.questionCount, 0, 0, 0)
            Message(header, message.query, message.answers, message.authority, message.additional)
          }*/
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

  def hostToRecords(qname: List[String], qtype: Int, qclass: Int) = {

    val domain = DNSCache.getDomain(qtype, qname)

    def hostname(qname: List[String], domain: ExtendedDomain) = {
      val hnm = qname.take(qname.indexOfSlice(domain.nameParts)).mkString(".")
      if (hnm.length == 0 || hnm == "@") domain.fullName else hnm
    }

    def resolveHost(host: Host, records: Array[AbstractRecord], cnames: Map[String, Array[AbstractRecord]], oldDomain: ExtendedDomain): Array[AbstractRecord] = {
      host match {
        case host: CnameHost => {
          if (!cnames.contains(host.hostname)) {
            try {
              val qname = if (host.hostname == "@") oldDomain.nameParts.toList else host.hostname.split("""\.""").toList
              val domain = if (host.hostname == "@") oldDomain else DNSCache.getDomain(qtype, qname)
              val newHost = if (host.hostname != "@") host else host.changeHostname(qname.mkString("."))
              records ++ domain.getHosts(hostname(qname, domain))
                .filter(h => h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString)
                .map(resolveHost(_, Array(), cnames + (host.hostname -> newHost.toRData), domain)).flatten
            } catch {
              // Cname points to an external domain, search cache
              case ex: DomainNotFoundException => records ++ host.toRData
            }
          } else records
        }
        case _ => records ++ cnames.values.flatten ++ host.toRData
      }
    }

    domain.getHosts(hostname(qname, domain)).filter(h => h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString).map { h =>
      resolveHost(h, Array(), Map(), domain)
    }.flatten

    //if(!records.isEmpty) records else throw new HostNotFoundException
  }

  def wildcardsToRecords(domain: ExtendedDomain, qname: List[String], qtype: Int, qclass: Int) = {
    val wc = qname.take(qname.indexOfSlice(domain.nameParts))

    if (wc.size + domain.nameParts.size <= 1) List()
    else {

      @tailrec
      def findWC(qname: List[String]): List[AbstractRecord] =
        if (qname.isEmpty) hostToRecords("*" :: domain.nameParts.toList, qtype, qclass)
        else {
          val rdata = hostToRecords("*" :: qname ++ domain.nameParts, qtype, qclass)
          if (!rdata.isEmpty) rdata else findWC(qname.tail)
        }

      findWC(wc.tail)
    }
  }
}
