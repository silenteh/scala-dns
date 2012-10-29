package handlers
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent

class DnsHandler extends SimpleChannelUpstreamHandler{
  
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent )  {
    
	 println(e.getMessage().toString())

     }
  
  

}