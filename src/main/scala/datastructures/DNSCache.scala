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
package datastructures
import scala.collection.immutable.TreeMap
import models.ExtendedDomain
import scala.collection.immutable.Map
import scala.Tuple2
import org.slf4j.LoggerFactory

//@TODO: I need to rewrite the cache and follow more the RFC1034: http://tools.ietf.org/html/rfc1034

object DNSCache {
  
  //val domains = TreeMap.empty[String,Map[String,Tuple2[Long,ExtendedDomain]]]
  //private val domains = TreeMap[String,Map[String,Tuple2[Long,ExtendedDomain]]]("." -> Map())
  
  val logger = LoggerFactory.getLogger("app")
  
  private val domains = TreeMap[String,Map[String, (Long, ExtendedDomain)]]("." -> Map())
  
  def getDomain(extension: String, name: String):Option[ExtendedDomain] = {
    domains.get(extension) match {
      case None => None
      case Some(domainsEntry) =>     	  
    	domainsEntry.get(name) match {
    	  case None => None
    	  case Some(t) => {
    		val diff = System.currentTimeMillis() - t._1 - t._2.ttl * 1000
    		if(diff > 0) Some(t._2) 
    		else None			  
    	  }
        }    		    	 
    }                
  }
  
  def setDomain(domain: ExtendedDomain) = {
    val storedMap = domains.get(domain.extension).getOrElse(Map())      
    val updatedMap = storedMap + (domain.name -> (System.currentTimeMillis(), domain))            
    domains + (domain.extension -> updatedMap)
    updatedMap
  }
  
  
  
}
