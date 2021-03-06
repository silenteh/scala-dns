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
package server.http.json

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.channel.Channel
import utils.{NotifyUtil, UriParser, SerialParser, Sha256Digest}
import datastructures.DNSCache
import datastructures.UserCache
import domainio.JsonIO
import server.http.HttpHandler
import models.ExtendedDomain
import domainio.DomainValidationService
import domainio.UserValidationService
import models.User
import datastructures.DNSAuthoritativeSection
import client.DNSClient
import configs.ConfigService
import scala.collection.JavaConversions._
import enums.RecordType
import org.slf4j.LoggerFactory

class JsonHttpHandler extends HttpHandler {
  val logger = LoggerFactory.getLogger("app")
  
  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    val channel = event.getChannel
    event.getMessage match {
      case request: HttpRequest => {
        request.getMethod match {
          case GET => handleGetRequest(request, channel)
          case POST => handlePostRequest(request, channel)
          case _ => throw new Error("Unsupported HTTP method")
        }
      }
      case _ => throw new Error("Unsupported request")
    }
  }

  def handleGetRequest(request: HttpRequest, channel: Channel) = {
    val path = UriParser.uriPath(request.getUri)
    val queryString = UriParser.uriQueryString(request)
    val content =
      if (path.length >= 1) {
        path(0) match {
          case "domains" => domainGetResponse(path, queryString)
          case "users" => userGetResponse
          case _ => throw new Error("Unsupported request")
        }
      } else throw new Error("Unsupported request")

    sendResponse(channel, content)
  }

  def handlePostRequest(request: HttpRequest, channel: Channel) = {
    val path = UriParser.uriPath(request.getUri)
    val content =
      if (path.length == 1 && path(0) == "domains") {
        domainPostResponse(request)
      } else if (path.length == 1 && path(0) == "users") {
        userPostResponse(request)
      } else {
        "{\"code\":1,\"message\":\"Error: Unknown request\"}"
      }
    sendResponse(channel, content)
  }
  
  private def domainGetResponse(path: List[String], queryString: Map[String, String]) = 
    if (path.length >= 2) {
      val extension = path(1).substring(path(1).lastIndexOf(".") + 1)
      val name = path(1).substring(0, path(1).lastIndexOf("."))
      //val domain = DNSCache.getDomains(extension)(name)
      val domain = DNSAuthoritativeSection.getDomains(extension)(name)
      "{\"domain\":" + JsonIO.Json.writeValueAsString(domain) + "}"
    } else if (!queryString.isEmpty && queryString.contains("menu")) {
      //val domains = DNSCache.getDomains.map { case (extension, value) =>
      val domains = DNSAuthoritativeSection.getDomains.map { case (extension, value) =>
        value.map { case (name, value) => (name + "." + extension) }
      }.flatten.toList
      JsonIO.Json.writeValueAsString(domains)
    } else {
      //val domains = DNSCache.getDomains.map { case (extension, value) =>
      val domains = DNSAuthoritativeSection.getDomains.map { case (extension, value) =>
        //value.map { case (name, (timestamp, domain)) => JsonIO.Json.writeValueAsString(domain) }
        value.map { case (name, domain) => JsonIO.Json.writeValueAsString(domain) }
      }.flatten.toList
      "{\"domains\":[" + domains.mkString(",") + "]}"
    }

  private def domainPostResponse(request: HttpRequest) = {
    val data = UriParser.postParams(request)
    if (data.get("delete") != None) {
      //DNSCache.removeDomain(data("delete").split("""\.""").toList)
      DNSAuthoritativeSection.removeDomain(data("delete").split("""\.""").toList)
      JsonIO.removeAuthData(data("delete"))
      "{\"code\":0,\"message\":\"Domain removed\"}"
    } else if (data.get("data") != None) {
      val domainCandidate = try {
        val domain = JsonIO.Json.readValue(data("data"), classOf[ExtendedDomain])
        domain.settings.foldRight(domain) {
          case (soa, domain) =>
            val newSoa = soa.updateSerial(
              if (soa.serial == null || soa.serial == "") SerialParser.generateNewSerial.toString
              else SerialParser.updateSerial(soa.serial).toString)
            domain.removeHost(soa).addHost(newSoa)
        }
      } catch {
        case ex: Exception => null
      }
      val replaceFilename = data.get("replace_filename").getOrElse(null)
      val (validcode, messages) = DomainValidationService.validate(domainCandidate, replaceFilename)
      if (validcode < 2) {
        if (replaceFilename != null) {
          //DNSCache.removeDomain(replaceFilename.split("""\.""").toList)
          DNSAuthoritativeSection.removeDomain(replaceFilename.split("""\.""").toList)
          JsonIO.removeAuthData(replaceFilename)
        }
        val domains = DomainValidationService.reorganize(domainCandidate)
        //DNSCache.setDomain(domains.head)
        DNSAuthoritativeSection.setDomain(domains.head)
        JsonIO.storeAuthData(domains.head)
        val response = "{\"code\":" + validcode + ",\"messages\":" + messages.mkString("[\"", "\",\"", "\"]") + ",\"data\":" + 
         JsonIO.Json.writeValueAsString(domains) + "}"
        NotifyUtil.notify(domainCandidate)
        
        response               

      } else {
        "{\"code\":" + validcode + ",\"messages\":" + messages.mkString("[\"", "\",\"", "\"]") + "}"
      }
    } else {
      "{\"code\":2,\"message\":\"Error: Unknown request\"}"
    }
  }
  
  private def userGetResponse = {
    val users = JsonIO.Json.writeValueAsString(UserCache.users.toArray)
    "{\"users\":%s}".format(users)
  }
  
  private def userPostResponse(request: HttpRequest) = {
    val data = UriParser.postParams(request)
    if (data.get("delete") != None) {
      UserCache.removeUser(data("delete"))
      JsonIO.updateUsers()
      "{\"code\":0,\"message\":\"User removed\"}"
    } else if (data.get("name") != None && data.get("digest") != None) {
      val replaceUsername = data.get("replace_filename").getOrElse(null)
      val user = try {
        val oldUser = if (replaceUsername == null) null else UserCache.findUser(data("name")) 
        if ((oldUser != null && oldUser.passwordDigest == data("digest")) || data("digest").isEmpty) new User(data("name"), data("digest"))
        else new User(data("name"), Sha256Digest(data("digest")))
      } catch {
        case ex: Exception => null
      }
      val (valid, messages) = UserValidationService.validate(user, replaceUsername)
      if (valid) {
        if (replaceUsername != null) {
          UserCache.removeUser(replaceUsername)
        }
        UserCache.addUser(user)
        JsonIO.updateUsers()
        "{\"code\":0,\"message\":\"Now look what you've done\",\"data\":" + JsonIO.Json.writeValueAsString(user) + "}"
      } else {
        "{\"code\":1,\"messages\":" + messages.mkString("[\"", "\",\"", "\"]") + "}"
      }
    } else {
      "{\"code\":1,\"message\":\"Error: Unknown request\"}"
    }
  }
}