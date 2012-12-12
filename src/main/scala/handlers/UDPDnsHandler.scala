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

class UDPDnsHandler extends SimpleChannelUpstreamHandler {

  val logger = LoggerFactory.getLogger("app")
  val UdpResponseMaxSize = ConfigService.config.getInt("udpResponseMaxSize")

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    logger.info("UDP. There is nothing else to say.")
    e.getMessage match {
      case message: Message => {
        logger.info(message.toString)
        logger.info("Request bytes: " + message.toByteArray.toList.toString)
        val response = DnsResponse(message, UdpResponseMaxSize)
        
        logger.debug("Compressed response length: " + response.length.toString)
        logger.debug("Compressed response bytes: " + response.toList.toString)
        e.getChannel.write(ChannelBuffers.copiedBuffer(response), e.getRemoteAddress)
      }
      case _ => {
        logger.error("Unsupported message type")
      }
    }
  }
}
