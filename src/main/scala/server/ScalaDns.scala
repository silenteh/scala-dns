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

import scala.collection.immutable.BitSet
import org.slf4j.LoggerFactory
import domainio.DomainIO
import payload.Message
import org.jboss.netty.buffer.ChannelBuffers
import records.A
import records.CNAME

object ScalaDns {
  
  val logger = LoggerFactory.getLogger("app")
  
  def main(args: Array[String]) = {
   
    /*val lst = List(0, 4, -123, 0, 0, 1, 0, 5, 0, 0, 0, 0, 3, 119, 119, 119, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 1, 0, 1, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 5, 0, 1, 0, 1, 81, -128, 0, 19, 3, 119, 119, 119, 9, 108, 105, 118, 101, 115, 99, 111, 114, 101, 3, 99, 111, 109, 0, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 1, 0, 1, 0, 1, 81, -128, 0, 4, -64, -88, 0, 1, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 5, 0, 1, 0, 1, 81, -128, 0, 13, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 1, 0, 1, 0, 1, 81, -128, 0, 4, -64, -88, 1, 12, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 5, 0, 1, 0, 1, 81, -128, 0, 12, 6, 97, 109, 97, 122, 111, 110, 3, 99, 111, 109, 0)
    val buf = ChannelBuffers.copiedBuffer(lst.map(_.toByte).toArray)
    
    val message = Message(buf)
    
    message.answers.foreach(_.rdata match {
      case rdata: A => logger.debug(rdata.address)
      case rdata: CNAME => logger.debug(rdata.record.map(new String(_, "UTF-8")).mkString("."))
      case _ => logger.debug("Irrelevant")
    })*/
    
    DomainIO.loadDomains()
    
    Bootstrap.start
    
//    var b = 166
//    val bits = new Array[Short](8)
    //println(b)
    //println(b >> 1)
    
//    for(i <- 0 to 7){
//      
//      val mod = b % 2
//      bits(i) = mod.toShort
////      println(i)
////      if(mod == 1) {
////        bits(i) = 1
////      } else {
////        bits(i) = 0
////      }
//      b >>= 1 
//    }
        
    
//    for(bit <- bits) {
//      print(bit)
//    }
//    println("")
//    println(toInt(bits))
//    
//    def toInt(bits: Array[Short]): Int = {
//    var n = 0
//    val limit = bits.length - 1
//    for(i <- 0 to limit) {
//      n = n + bits(i) * (scala.math.pow(2, i)).toInt
//    }    
//    n    
//  }
   
    
  }
}
