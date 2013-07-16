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
import domainio.JsonIO
import utils.UserCreator
import datastructures.DNSCache
import enums.RecordType
import server.dns.DnsLookupService
import payload._
import server.dns.DnsResponseBuilder
import org.jboss.netty.buffer.ChannelBuffers
import datastructures.DNSAuthoritativeSection
import configs.ConfigService
import collection.JavaConversions._
import client.DNSClient

object ScalaDns {
  
  val logger = LoggerFactory.getLogger("app")
  
  def main(args: Array[String]) = {

    JsonIO.loadData
	JsonIO.loadUsers()
    
	/*val domain = DNSCache.getDomain(RecordType.NS.id, List("blah", "blah"))
	logger.debug(domain.nameservers.map(_.hostnames.toList.toString).toList.toString)
	
	val records = DnsLookupService.hostToRecords(List("blah", "blah"), RecordType.NS.id, 1)
	logger.debug(records.toString)*/
    
    if(args.exists(_.startsWith("-user="))) {
      val userParts = args.find(_.startsWith("-user=")).get.substring(6).split(""",""")
      if(userParts.length != 2) println("Invalid arguments. Usage: -user=<username>,<password>")
      else println(UserCreator(userParts(0), userParts(1)))
    }
	
    val questionData = DNSAuthoritativeSection.getDomainNames.map(n => (n.split("""\.""").toList.filterNot(_.isEmpty), RecordType.SOA.id, 1)).toList
    ConfigService.config.getStringList("zoneTransferAllowedIps").foreach {ip =>
      logger.debug("Message is about to be sent")  
      DNSClient.sendNotify(ip, 53, questionData)(message => Unit)
    }
    
    if(args.isEmpty || args.contains("-start")) {
	  Bootstrap.start
    }
    
    //val domains = DNSCache.getDomains.map {case(key, value) => (key, value.filterNot(_._1 == "mail.livescore"))}
    
    
    
    //val zoneRecords = DnsLookupService.zoneToRecords("www2" :: "example" :: "com" :: Nil, 1)
    //logger.debug(zoneRecords.toList.mkString("\n"))
    
    /*val addressHost = new AddressHost("IN", "www", Array(new WeightedIP(1, "192.168.1.12"), new WeightedIP(1, "192.168.0.1")))
    val domain = new ExtendedDomain("example.com.", 86400L, null, null, null, Array(addressHost), null, null, null, null, null)
    logger.debug(Json.generate(domain))*/

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
