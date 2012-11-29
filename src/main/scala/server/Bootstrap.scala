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
package server

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory
import org.jboss.netty.bootstrap.ConnectionlessBootstrap
import java.net.InetAddress
import pipeline.UDPDnsPipeline

object Bootstrap {

  // get the number of CPU cores
  val cores = Runtime.getRuntime().availableProcessors() + 1
  
  // Java Executors
  val executorTCP = Executors.newFixedThreadPool(cores)
  val executorUDP = Executors.newFixedThreadPool(cores)
  
  // ### TCP
  // bootstraps so they can be closed down gracefully
  val tcpFactory = new NioServerSocketChannelFactory(executorTCP, executorTCP)
  val tcpBootstrap = new ServerBootstrap(tcpFactory)
  
  
  // ### UDP
  val udpFactory = new NioDatagramChannelFactory(executorUDP)
  val udpBootstrap = new ConnectionlessBootstrap(udpFactory)
  
  
  // Starts both services
  def start() {
    //startTCP
    startUDP
  } 
  
  def stop() {
    //stopTCP
    stopUDP
  }
  	
  
  private def startTCP() {

    // Configure the TCP pipeline factory.
    tcpBootstrap.setPipelineFactory(new UDPDnsPipeline())
    

    // Bind and start to accept incoming connections.
    // we need tor efactor this to set it up via config
    tcpBootstrap.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 53))
    
  }
  
  private def startUDP() {
    
    // bind the server to an address and port
    // we need to refactor this to set it up via config
    //bootstrap.bind(new InetSocketAddress(InetAddress.getByName("192.168.1.100"), 8080));
    //udpBootstrap.setOption("localAddress", new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 53));
    udpBootstrap.setOption("tcpNoDelay", true);
 	udpBootstrap.setOption("receiveBufferSize", 1048576);
    udpBootstrap.setPipelineFactory(new UDPDnsPipeline())
    
    udpBootstrap.bind(new InetSocketAddress(53))
    
    
  }
  
  private def stopTCP() {
    tcpBootstrap.releaseExternalResources()
  }
  
  private def stopUDP() {
    udpBootstrap.releaseExternalResources()
  }
  
  
  
  
  
}
