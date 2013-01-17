package server.http.file

import tools.PipelineModifier
import org.jboss.netty.channel.ChannelPipeline

class FileHttpModifier extends PipelineModifier {
  val handler = new FileHttpHandler
  
  def modifyPipeline(pipeline: ChannelPipeline) =
    if(pipeline.get(getName) == null) pipeline.addLast(getName, handler)

  def getChannelHandler = handler
  
  def getName: String = FileHttpModifier.name
}

object FileHttpModifier {
  val name = "filehttp"
}