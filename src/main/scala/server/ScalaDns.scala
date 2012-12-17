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
import handlers.DnsLookupService
import models.WeightedIP
import models.AddressHost
//import com.codahale.jerkson.Json
import models.ExtendedDomain

object ScalaDns {
  
  val logger = LoggerFactory.getLogger("app")
  
  def main(args: Array[String]) = {

    DomainIO.loadDomains()
    
    //val zoneRecords = DnsLookupService.zoneToRecords("www2" :: "example" :: "com" :: Nil, 1)
    //logger.debug(zoneRecords.toList.mkString("\n"))
    
    /*val addressHost = new AddressHost("IN", "www", Array(new WeightedIP(1, "192.168.1.12"), new WeightedIP(1, "192.168.0.1")))
    val domain = new ExtendedDomain("example.com.", 86400L, null, null, null, Array(addressHost), null, null, null, null, null)
    logger.debug(Json.generate(domain))*/
    
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
