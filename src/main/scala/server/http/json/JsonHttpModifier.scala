package server.http.json

import pipeline.PipelineModifier
import org.jboss.netty.channel.ChannelPipeline

class JsonHttpModifier extends PipelineModifier {
  val handler = new JsonHttpHandler
  
  def modifyPipeline(pipeline: ChannelPipeline) =
    if(pipeline.get(getName) == null) pipeline.addLast(getName, handler)

  def getChannelHandler = handler
  
  def getName: String = JsonHttpModifier.name
}

object JsonHttpModifier {
  val name = "jsonhttp"
}