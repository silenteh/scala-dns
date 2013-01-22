/*******************************************************************************
 * Copyright 2012 silenteh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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