package client

import org.jboss.netty.bootstrap.ConnectionlessBootstrap
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory
import java.util.concurrent.Executors
import pipeline.ClientUDPDnsPipeline
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.DatagramChannel
import payload.Message
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener

object UDPDnsClient {

  val bootstrap = new ConnectionlessBootstrap(new NioDatagramChannelFactory(Executors.newCachedThreadPool))
  bootstrap.setPipelineFactory(new ClientUDPDnsPipeline)
  bootstrap.setOption("reuseAddress", true);
  bootstrap.setOption("tcpNoDelay", true);
  bootstrap.setOption("broadcast", "false");
  bootstrap.setOption("sendBufferSize", 65536);
  bootstrap.setOption("receiveBufferSize", 65536);
                
  val channel = bootstrap.bind(new InetSocketAddress(0)).asInstanceOf[DatagramChannel];
  
  /*def send(address: String, port: Int, message: Message) = {
    val bufferedMessage = ChannelBuffers.copiedBuffer(message.toCompressedByteArray(Array(), Map())._1)
    val future = channel.write(bufferedMessage, new InetSocketAddress(address, port))
  }*/
  
  def send(address: String, port: Int, message: Message)(callback: ChannelFuture => Unit) = {
    val bufferedMessage = ChannelBuffers.copiedBuffer(message.toCompressedByteArray(Array(), Map())._1)
    val future = channel.write(bufferedMessage, new InetSocketAddress(address, port))
    future.addListener(new ChannelFutureListener() {
      override def operationComplete(cf: ChannelFuture) = callback(cf)
    })
  }
  
  def stop = {
    channel.close
    bootstrap.releaseExternalResources
  }
  
}