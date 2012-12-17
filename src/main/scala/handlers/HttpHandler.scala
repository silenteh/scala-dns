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
package handlers

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._
import utils.UriParser
import datastructures.DNSCache
import domainio.DomainIO
import enums.RecordType

class HttpHandler extends SimpleChannelUpstreamHandler {
  
  val logger = LoggerFactory.getLogger("app")
  
  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    val channel = event.getChannel
    event.getMessage match {
      case request: HttpRequest => {
        val content = request.getMethod match {
          case GET => handleGetRequest(request)
          case POST => handlePostRequest(request)
          case _ => throw new Error("Unsupported HTTP method")
        }
        val contentBuffer = ChannelBuffers.copiedBuffer(content.getBytes)
        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        response.setHeader(CONTENT_TYPE, "application/json")
        response.setContent(contentBuffer)
        setContentLength(response, contentBuffer.readableBytes)
        channel.write(response).addListener(ChannelFutureListener.CLOSE)
      }
      case _ => Unit
    }
  }
  
  override def exceptionCaught(context: ChannelHandlerContext, event: ExceptionEvent) = {
    event.getCause.printStackTrace
    val contentBuffer = ChannelBuffers.copiedBuffer("Not found".getBytes)
    val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
    response.setHeader(CONTENT_TYPE, "text/plain; charset=utf-8")
    response.setContent(contentBuffer)
    setContentLength(response, contentBuffer.readableBytes)
    event.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
  
  private def handleGetRequest(request: HttpRequest) = {
    val path = UriParser.uriPath(request.getUri)
    if(path.isEmpty) {
      val domains = DNSCache.getDomains.map { case(top, value) => 
        value.map { case (middle, (updated, domain)) => DomainIO.Json.writeValueAsString(domain)}
      }.flatten.toList
      "{\"domains\":[" + domains.mkString(",") + "]}"
    } else if(path.length == 2 && path(0) == "domain") {
      val extension = path(1).substring(path(1).lastIndexOf(".") + 1)
      val name = path(1).substring(0, path(1).lastIndexOf("."))
      val domain = DNSCache.getDomains(extension)(name)
      "{\"domain\":" + DomainIO.Json.writeValueAsString(domain) + "}"
    } else throw new Error("Unknown request")
  }
  
  private def handlePostRequest(request: HttpRequest) = {
    ""
  }
  
}