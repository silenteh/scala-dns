package server.http

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpResponse
import java.net.URLDecoder
import java.io.UnsupportedEncodingException
import java.io.File
import javax.activation.MimetypesFileTypeMap
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

abstract class HttpHandler extends SimpleChannelUpstreamHandler {
  def sendResponse(channel: Channel, content: String) = {
    val contentBuffer = ChannelBuffers.copiedBuffer(content.getBytes)
    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    response.setHeader(CONTENT_TYPE, "application/json")
    response.setContent(contentBuffer)
    setContentLength(response, contentBuffer.readableBytes)
    channel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
  
  def sendError(context: ChannelHandlerContext, status: HttpResponseStatus) = {
    val response = new DefaultHttpResponse(HTTP_1_1, status)
    response.setHeader(CONTENT_TYPE, "text_plain, charset=utf-8")
    response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString + "\r\n", CharsetUtil.UTF_8))
    context.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
}

object HttpHandler extends SimpleChannelUpstreamHandler {
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
  
  def sendNotModified(context: ChannelHandlerContext) = {
    val response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED)
    setDateHeader(response)
    context.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
  
  def sendError(context: ChannelHandlerContext, status: HttpResponseStatus) = {
    val response = new DefaultHttpResponse(HTTP_1_1, status)
    response.setHeader(CONTENT_TYPE, "text_plain, charset=utf-8")
    response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString + "\r\n", CharsetUtil.UTF_8))
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