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
package server.http

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._
import utils.UriParser
import org.jboss.netty.channel.Channel
import datastructures.UserCache
import models.User
import sun.misc.BASE64Decoder
import org.jboss.netty.channel.ChannelPipeline
import tools.PipelineModifier

class DefaultAuthHttpHandler(modifiers: Map[String, PipelineModifier]) extends SimpleChannelUpstreamHandler {

  val logger = LoggerFactory.getLogger("app")

  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    val channel = event.getChannel
    event.getMessage match {
      case request: HttpRequest => 
        authenticate(request) (
          request => sendAuthentication(channel),
          user => {
            val modifier = getModifier(request.getUri)
            if (!modifier.getName.equals(context.getAttachment)) {
              clearLastModifier(context.getPipeline)
              modifier.modifyPipeline(context.getPipeline)
              context.setAttachment(modifier.getName)
            }
          }
        )
      case _ => context.sendDownstream(event)
    }
    context.sendUpstream(event)
  }
  
  override def exceptionCaught(context: ChannelHandlerContext, event: ExceptionEvent) = {
    event.getCause.printStackTrace
    HttpHandler.sendError(context, NOT_FOUND)
  }

  private def authenticate(request: HttpRequest)(failure: HttpRequest => Unit, success: User => Unit) = {
    val authHeader = request.getHeader("Authorization")
    if (authHeader == null) {
      failure(request)
    } else {
      val authParts = authHeader.split(""" """)
      val credentials = new String(new BASE64Decoder().decodeBuffer(authParts(1))).split("""\:""")
      val user = UserCache.findUser(credentials(0), credentials(1))
      if (user == null) failure(request)
      else success(user)
    }
  }
  
  private def sendAuthentication(channel: Channel) = {
    val response = new DefaultHttpResponse(HTTP_1_1, UNAUTHORIZED)
    response.setHeader("WWW-Authenticate", "Basic realm=\"127.0.0.1\"")
    setContentLength(response, 0)
    channel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
  
  def clearLastModifier(pipeline: ChannelPipeline) = if (this != pipeline.getLast) pipeline.removeLast
  
  def getModifier(uri: String) = {
    val path = UriParser.uriPath(uri)
    val modifierName = 
      if(path.length >= 1 && (path(0) == "domain" || path(0) == "domains" || path(0) == "users")) "jsonhttp"
      else "filehttp"
    modifiers(modifierName)
  }
}