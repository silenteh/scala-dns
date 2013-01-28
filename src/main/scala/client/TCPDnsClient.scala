package client

import scala.concurrent.ExecutionContext.Implicits.global
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
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import pipeline.ClientTCPDnsPipeline
import payload.RRData
import scala.concurrent.Future
import org.slf4j.LoggerFactory

object TCPDnsClient {
  
  val logger = LoggerFactory.getLogger("app")
  
  def send(address: String, port: Int, message: Message)(callback: ChannelFuture => Unit) = {
    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool,
      Executors.newCachedThreadPool))
    bootstrap.setPipelineFactory(new ClientTCPDnsPipeline)
    
    val connectionFuture = bootstrap.connect(new InetSocketAddress(address, port))
    connectionFuture.addListener(new ChannelFutureListener() {
      override def operationComplete(cf: ChannelFuture) = {
        val channel = cf.getChannel()
        val messageBytes = message.toCompressedByteArray(Array(), Map())._1
        val bufferedMessage = ChannelBuffers.copiedBuffer(RRData.shortToBytes(messageBytes.length.toShort) ++ messageBytes)
        val future = channel.write(bufferedMessage, new InetSocketAddress(address, port))
        future.addListener(new ChannelFutureListener() {
          override def operationComplete(cf: ChannelFuture) = {
            callback(cf)
            channel.getCloseFuture.addListener(new ChannelFutureListener() {
              override def operationComplete(cf: ChannelFuture) = Future {
                channel.close
                bootstrap.releaseExternalResources();
              }
            })
          }
        })
      }
    })
  }
}