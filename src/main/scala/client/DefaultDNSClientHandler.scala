package client

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import payload.Message

class DefaultDNSClientHandler extends SimpleChannelUpstreamHandler {
  
  val logger = LoggerFactory.getLogger("app")
  
  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) = {
    event.getMessage match {
      case message: Message => {
        logger.debug("Response received")
        DNSClient.processResponse(message)
      }
      case _ => logger.debug("Error, error, error")
    }
  }
  
  override def exceptionCaught(context: ChannelHandlerContext, event: ExceptionEvent) = {
    logger.debug("Exception caught")
    logger.error(event.getCause.getMessage)
    logger.error(event.getCause.getStackTraceString)
  }
  
}