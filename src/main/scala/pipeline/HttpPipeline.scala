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
package pipeline

import org.jboss.netty.channel.ChannelPipelineFactory
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import server.http.HttpHandler
import server.http.DefaultAuthHttpHandler
import server.http.HttpModifiers

class HttpPipeline extends ChannelPipelineFactory {
  
  val logger = LoggerFactory.getLogger("app")
  val modifiers = HttpModifiers()
  
  def getPipeline(): ChannelPipeline = {
    logger.info("PIPELINING.........")
    val pipeline = Channels.pipeline
    pipeline.addLast("decoder", new HttpRequestDecoder)
    //pipeline.addLast("aggregator", new HttpChunkAggregator(ServerPipelineFactory.MaxFrameSize))
    pipeline.addLast("encoder", new HttpResponseEncoder)
    pipeline.addLast("http_handler",new DefaultAuthHttpHandler(modifiers))
    pipeline
  }
  
}