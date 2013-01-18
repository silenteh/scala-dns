package server.http.file

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.RandomAccessFile
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.channel.DefaultFileRegion
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.ChannelFuture
import java.io.FileNotFoundException
import scala.annotation.tailrec
import server.http.HttpHandler
import org.slf4j.LoggerFactory

class FileHttpHandler extends HttpHandler {

  val logger = LoggerFactory.getLogger("app")

  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    val channel = event.getChannel
    event.getMessage match {
      case request: HttpRequest => {
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
      case _ => throw new Error("Unsupported request")
    }
  }

  override def exceptionCaught(context: ChannelHandlerContext, event: ExceptionEvent) = {
    event.getCause.printStackTrace
    HttpHandler.sendError(context, INTERNAL_SERVER_ERROR)
  }

}