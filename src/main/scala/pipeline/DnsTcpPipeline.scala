package pipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder
import org.jboss.netty.handler.codec.frame.Delimiters
import org.jboss.netty.handler.codec.string.StringEncoder
import org.jboss.netty.handler.codec.string.StringDecoder
import scalaframes.DnsMessageDecoder
import handlers.DnsHandler

class DnsTcpPipeline extends ChannelPipelineFactory {

  override def getPipeline = {
    println("PIPELINING.........")
    // Create a default pipeline implementation.
    val pipeline = org.jboss.netty.channel.Channels.pipeline

    // Add the text line codec combination first,
    val frameDecoder = new DnsMessageDecoder
    pipeline.addLast("framer", frameDecoder)
    pipeline.addLast("decoder", new StringDecoder)
    pipeline.addLast("encoder", new StringEncoder)
    pipeline.addLast("dns_handler",new DnsHandler)

    // and then business logic.
    //pipeline.addLast("handler", new TelnetServerHandler)

    pipeline
  }


}