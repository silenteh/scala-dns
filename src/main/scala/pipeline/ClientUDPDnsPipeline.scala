package pipeline

import org.jboss.netty.channel.ChannelPipelineFactory
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelPipeline
import scalaframes.UDPDnsMessageDecoder
import org.jboss.netty.handler.codec.string.StringDecoder
import org.jboss.netty.handler.codec.string.StringEncoder
import client.DefaultDNSClientHandler

class ClientUDPDnsPipeline extends ChannelPipelineFactory {

  val logger = LoggerFactory.getLogger("app")
  
  def getPipeline(): ChannelPipeline = {
    logger.info("pipelining.........")
    // Create a default pipeline implementation.
    val pipeline = org.jboss.netty.channel.Channels.pipeline

    // Add the text line codec combination first,
    val frameDecoder = new UDPDnsMessageDecoder
    pipeline.addLast("framer", frameDecoder)
    pipeline.addLast("decoder", new StringDecoder)
    pipeline.addLast("encoder", new StringEncoder)
    pipeline.addLast("dns_handler", new DefaultDNSClientHandler)

    // and then business logic.
    //pipeline.addLast("handler", new TelnetServerHandler)

    pipeline
  }


}