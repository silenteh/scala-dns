package tools

import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelHandler

trait PipelineModifier {
  def modifyPipeline(pipeline: ChannelPipeline): Unit
  
  def getName: String
  
  def getChannelHandler: ChannelHandler
}