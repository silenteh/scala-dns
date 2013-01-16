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
import javax.activation.MimetypesFileTypeMap
import java.net.URLDecoder
import java.io.File
import java.io.UnsupportedEncodingException
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.HttpResponse
import java.util.GregorianCalendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import java.io.RandomAccessFile
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.ChannelFuture
import java.io.FileNotFoundException
import org.jboss.netty.channel.Channel
import scala.annotation.tailrec
import models.ExtendedDomain
import domainio.DomainValidationService
import models.SoaHost
import utils.SerialParser

class HttpHandler extends SimpleChannelUpstreamHandler {

  val logger = LoggerFactory.getLogger("app")

  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    val channel = event.getChannel
    event.getMessage match {
      case request: HttpRequest => {
        val content = request.getMethod match {
          case GET => try {
            handleGetRequest(request, channel)
          } catch {
            case ex: Error => serveFile(request, context, channel)
          }
          case POST => handlePostRequest(request, channel)
          case _ => throw new Error("Unsupported HTTP method")
        }
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

  private def handleGetRequest(request: HttpRequest, channel: Channel) = {
    val path = UriParser.uriPath(request.getUri)
    val queryString = UriParser.uriQueryString(request)
    val content =
      if (path.length == 1 && path(0) == "domains" && !queryString.isEmpty && queryString.contains("menu")) {
        val domains = DNSCache.getDomains.map {
          case (extension, value) => value.map { case (name, value) => (name + "." + extension) }
        }.flatten.toList
        DomainIO.Json.writeValueAsString(domains)

      } else if (path.length == 1 && path(0) == "domains") {
        val domains = DNSCache.getDomains.map {
          case (extension, value) => value.map { case (name, (timestamp, domain)) => DomainIO.Json.writeValueAsString(domain) }
        }.flatten.toList
        "{\"domains\":[" + domains.mkString(",") + "]}"

      } else if (path.length == 2 && path(0) == "domains") {
        val extension = path(1).substring(path(1).lastIndexOf(".") + 1)
        val name = path(1).substring(0, path(1).lastIndexOf("."))
        val domain = DNSCache.getDomains(extension)(name)
        "{\"domain\":" + DomainIO.Json.writeValueAsString(domain) + "}"

      } else throw new Error("Unknown request")

    val contentBuffer = ChannelBuffers.copiedBuffer(content.getBytes)
    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    response.setHeader(CONTENT_TYPE, "application/json")
    response.setContent(contentBuffer)
    setContentLength(response, contentBuffer.readableBytes)
    channel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  private def handlePostRequest(request: HttpRequest, channel: Channel) = {
    val path = UriParser.uriPath(request.getUri)
    val content =
      if (path.length == 1 && path(0) == "domains") {
        val data = UriParser.postParams(request)
        if (data.get("delete") != None) {
          DNSCache.removeDomain(data("delete").split("""\.""").toList)
          DomainIO.removeDomain(data("delete"))
          "{\"code\":0,\"message\":\"Domain removed\"}"
        } else if (data.get("data") != None) {
          val domainCandidate = try {
            val domain = DomainIO.Json.readValue(data("data"), classOf[ExtendedDomain])
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
          val (valid, messages) = DomainValidationService.validate(domainCandidate, replaceFilename)
          if (valid) {
            if(replaceFilename != null) {
              DNSCache.removeDomain(replaceFilename.split("""\.""").toList)
              DomainIO.removeDomain(replaceFilename)
            }
            val domains = DomainValidationService.reorganize(domainCandidate)
            DNSCache.setDomain(domains.head)
            DomainIO.storeDomain(domains.head)
            "{\"code\":0,\"message\":\"Now look what you've done\",\"data\":" + DomainIO.Json.writeValueAsString(domains) + "}"
          } else {
            "{\"code\":1,\"messages\":" + messages.mkString("[\"", "\",\"", "\"]") + "}"
          }
        } else {
          "{\"code\":1,\"message\":\"Error: Unknown request\"}"
        }
      } else {
        "{\"code\":1,\"message\":\"Error: Unknown request\"}"
      }

    val contentBuffer = ChannelBuffers.copiedBuffer(content.getBytes)
    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    response.setHeader(CONTENT_TYPE, "application/json")
    response.setContent(contentBuffer)
    setContentLength(response, contentBuffer.readableBytes)
    channel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  private def serveFile(request: HttpRequest, context: ChannelHandlerContext, channel: Channel) = {
    val path = HttpHandler.sanitizeURI(request.getUri)
    if (path == null) HttpHandler.sendError(context, FORBIDDEN)
    else {
      val file = {
        val file = new File(path)
        if (!file.isHidden && file.exists && file.isFile) file
        else {
          val pathToFile = if (path.endsWith("/")) path else path + "/"

          @tailrec
          def findIndexFile(files: List[String]): File =
            if (files.isEmpty) file
            else {
              val indexFile = new File(pathToFile + files.head)
              if (indexFile.exists && !indexFile.isHidden && indexFile.isFile) indexFile
              else findIndexFile(files.tail)
            }

          findIndexFile(HttpHandler.indexFiles)
        }
      }
      if (file.isHidden || !file.exists) HttpHandler.sendError(context, NOT_FOUND)
      else if (!file.isFile) HttpHandler.sendError(context, FORBIDDEN)
      else {
        val ifModifiedSince = request.getHeader(IF_MODIFIED_SINCE)
        val isModified = if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
          val dateFormatter = new SimpleDateFormat(HttpHandler.HttpDateFormat, Locale.US)
          val ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince)
          !(ifModifiedSinceDate.getTime / 1000 == file.lastModified / 1000)
        } else true
        if (!isModified) HttpHandler.sendNotModified(context)
        else {
          try {
            val raf = new RandomAccessFile(file, "r")
            val fileLength = raf.length
            val response = new DefaultHttpResponse(HTTP_1_1, OK)
            setContentLength(response, fileLength)
            HttpHandler.setContentTypeHeader(response, file)
            HttpHandler.setDateAndCacheHeader(response, file)

            channel.write(response)

            val writeFuture = if (channel.getPipeline.get(classOf[SslHandler]) != null) {
              channel.write(new ChunkedFile(raf, 0, fileLength, 8192))
            } else {
              val region = new DefaultFileRegion(raf.getChannel, 0, fileLength)
              val wf = channel.write(region)
              wf.addListener(new ChannelFutureProgressListener() {
                def operationComplete(future: ChannelFuture) = {
                  if (future.isSuccess) logger.info("%s completed".format(path))
                  else logger.error("%s failed".format(path))
                  region.releaseExternalResources
                }
                def operationProgressed(future: ChannelFuture, amount: Long, current: Long, total: Long) =
                  logger.info("%s: %d / %d (+%d) %s".format(path, current, total, amount, channel.isWritable.toString))
              })
              wf
            }

            if (isKeepAlive(request)) writeFuture.addListener(ChannelFutureListener.CLOSE)
          } catch {
            case e: FileNotFoundException => HttpHandler.sendError(context, NOT_FOUND)
          }
        }
      }
    }
  }
}

object HttpHandler extends SimpleChannelUpstreamHandler {
  val logger = LoggerFactory.getLogger("app")
  val HttpDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"
  val HttpDateGmtTimezone = "GMT"
  val HttpCacheSeconds = 60
  val mimeTypesMap = new MimetypesFileTypeMap
  val rootDir = "/public"
  val indexFiles = "index.html" :: "index.htm" :: Nil
  mimeTypesMap.addMimeTypes("application/x-javascript js JS")
  mimeTypesMap.addMimeTypes("text/css css CSS")

  def sanitizeURI(uri: String) = {
    val decUri = try {
      URLDecoder.decode(uri, "UTF-8").replace('/', File.separatorChar)
    } catch {
      case e: UnsupportedEncodingException => try {
        URLDecoder.decode(uri, "ISO-8859-1").replace('/', File.separatorChar)
      } catch {
        case e: UnsupportedEncodingException => throw new Error
      }
    }
    if (decUri.contains(File.separator + ".") || decUri.contains("." + File.separator) || decUri.startsWith(".") || decUri.endsWith(".")) null
    else System.getProperty("user.dir") + File.separator + rootDir + uri
  }

  def sendError(context: ChannelHandlerContext, status: HttpResponseStatus) = {
    val response = new DefaultHttpResponse(HTTP_1_1, status)
    response.setHeader(CONTENT_TYPE, "text_plain, charset=utf-8")
    response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString + "\r\n", CharsetUtil.UTF_8))
    context.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  def sendNotModified(context: ChannelHandlerContext) = {
    val response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED)
    setDateHeader(response)
    context.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  def setDateHeader(response: HttpResponse) = {
    val dateFormatter = new SimpleDateFormat(HttpDateFormat, Locale.US)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HttpDateGmtTimezone))

    val time = new GregorianCalendar
    response.setHeader(DATE, dateFormatter.format(time.getTime))
  }

  def setDateAndCacheHeader(response: HttpResponse, fileToCache: File) = {
    val dateFormatter = new SimpleDateFormat(HttpDateFormat, Locale.US)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HttpDateGmtTimezone))

    val time = new GregorianCalendar
    response.setHeader(DATE, dateFormatter.format(time.getTime))
    time.add(Calendar.SECOND, HttpCacheSeconds)
    response.setHeader(EXPIRES, dateFormatter.format(time.getTime))
    response.setHeader(CACHE_CONTROL, "private, max-age=" + HttpCacheSeconds)
    response.setHeader(LAST_MODIFIED, dateFormatter.format(fileToCache.lastModified))
  }

  def setContentTypeHeader(response: HttpResponse, file: File) = {
    response.setHeader(CONTENT_TYPE, mimeTypesMap.getContentType(file))
  }

}