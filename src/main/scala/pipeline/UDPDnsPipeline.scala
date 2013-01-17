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
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder
import org.jboss.netty.handler.codec.frame.Delimiters
import org.jboss.netty.handler.codec.string.StringEncoder
import org.jboss.netty.handler.codec.string.StringDecoder
import scalaframes.UDPDnsMessageDecoder
import org.slf4j.LoggerFactory
import org.jboss.netty.channel.ChannelPipeline
import server.dns.UDPDnsHandler

class UDPDnsPipeline extends ChannelPipelineFactory {

  val logger = LoggerFactory.getLogger("app")
  
  def getPipeline(): ChannelPipeline = {
    logger.info("PIPELINING.........")
    // Create a default pipeline implementation.
    val pipeline = org.jboss.netty.channel.Channels.pipeline

    // Add the text line codec combination first,
    val frameDecoder = new UDPDnsMessageDecoder
    pipeline.addLast("framer", frameDecoder)
    pipeline.addLast("decoder", new StringDecoder)
    pipeline.addLast("encoder", new StringEncoder)
    pipeline.addLast("dns_handler",new UDPDnsHandler)

    // and then business logic.
    //pipeline.addLast("handler", new TelnetServerHandler)

    pipeline
  }


}
