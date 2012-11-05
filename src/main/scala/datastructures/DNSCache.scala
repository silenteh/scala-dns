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

// TODO: I need to rewrite the cache and follow more the RFC1034: http://tools.ietf.org/html/rfc1034

object DNSCache {
  

  //val domains = TreeMap.empty[String,Map[String,Tuple2[Long,ExtendedDomain]]]
  private val domains = TreeMap[String,Map[String,Tuple2[Long,ExtendedDomain]]]("." -> Map.empty[String, Tuple2[Long,ExtendedDomain]])
  
  def getDomain(extension: String, name: String):Option[ExtendedDomain] = {
    val domainMap = domains.get(extension)
    val domain = domainMap match {
    	case None => None
    	case m: Some[Map[String,Tuple2[Long,ExtendedDomain]]] => {    	  
    		val storedMap = m.get.get(name)
    		  storedMap match {
    			case None => None
    			case t: Some[Tuple2[Long,ExtendedDomain]] => {
    			  val diff = System.currentTimeMillis() - t.get._1 - t.get._2.ttl * 1000
    			  if(diff > 0) {
    				  Some(t.get._2)
    			  } else {
    			    None
    			  }    			  
    			}
    		}    		    	 
    	}    	      	  
    }    
    domain            
  }
  
  def setDomain(domain: ExtendedDomain) = {
    val storedMap = domains.get(domain.extension).getOrElse(Map.empty[String, Tuple2[Long,ExtendedDomain]])      
    val updatedMap = storedMap + (domain.name -> Tuple2[Long,ExtendedDomain](System.currentTimeMillis(),domain))            
    domains + (domain.extension -> updatedMap)   
    updatedMap
  }
  
  
  
}
