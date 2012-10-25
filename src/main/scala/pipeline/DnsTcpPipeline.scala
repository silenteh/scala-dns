package pipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder
import org.jboss.netty.handler.codec.frame.Delimiters
import org.jboss.netty.handler.codec.string.StringEncoder
import org.jboss.netty.handler.codec.string.StringDecoder

class DnsTcpPipeline extends ChannelPipelineFactory {

  override def getPipeline = {
    // Create a default pipeline implementation.
    val pipeline = org.jboss.netty.channel.Channels.pipeline

    // Add the text line codec combination first,
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
      8192, (Delimiters.lineDelimiter): _*))
    pipeline.addLast("decoder", new StringDecoder)
    pipeline.addLast("encoder", new StringEncoder)

    // and then business logic.
    //pipeline.addLast("handler", new TelnetServerHandler)

    pipeline
  }


}