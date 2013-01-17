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
import scala.annotation.tailrec
import domainio.JsonIO

//@TODO: I need to rewrite the cache and follow more the RFC1034: http://tools.ietf.org/html/rfc1034

object DNSCache {
  
  //val domains = TreeMap.empty[String,Map[String,Tuple2[Long,ExtendedDomain]]]
  //private val domains = TreeMap[String,Map[String,Tuple2[Long,ExtendedDomain]]]("." -> Map())
  
  val logger = LoggerFactory.getLogger("app")
  
  //find a mutable equivalent to TreeMap
  private var domains = TreeMap[String,Map[String, (Long, ExtendedDomain)]]("." -> Map())
  
  def findDomain(typ: Int, extension: String, name: String): Option[ExtendedDomain] = 
    findDomain(typ, (name.split("""\.""") :+ extension).toList)          
  
  def findDomain(typ: Int, parts: String*): Option[ExtendedDomain] = 
    findDomain(typ, parts.toList)
    
  def findDomain(typ: Int, parts: List[String]): Option[ExtendedDomain] = {
    
    @tailrec
    def findDomainName(storedMap: Map[String, (Long, ExtendedDomain)], name: Seq[String]): Option[ExtendedDomain] = 
      if(name.isEmpty) None
      else storedMap.get(name.mkString(".")) match {
        case None => findDomainName(storedMap, name.tail)
        case Some((timestamp, domain)) => {
          val diff = timestamp + domain.ttl * 1000 - System.currentTimeMillis()
          if((parts.size - 1 != name.size || domain.hasRootEntry(typ)) && diff > 0) Some(domain)
          else findDomainName(storedMap, name.tail)
        }
      }
    
    val extension = parts.reverse.head
    val name = parts.take(parts.size - 1)
    domains.get(extension) match {
      case None => None
      case Some(storedMap) => 
        findDomainName(storedMap, name)
    }
  }
  
  def getDomain(typ: Int, extension: String, name: String): ExtendedDomain = 
    getDomain(typ, (name.split("""\.""") :+ extension).toList)
  
  def getDomain(typ: Int, parts: String*): ExtendedDomain = 
    getDomain(typ, parts.toList)
  
  def getDomain(typ: Int, parts: List[String]): ExtendedDomain = 
    findDomain(typ, parts) match {
      case Some(domain) => domain
      case None => throw new DomainNotFoundException
    }
  
  def setDomain(domain: ExtendedDomain) = {
    val storedMap = domains.get(domain.extension).getOrElse(Map())      
    val updatedMap = storedMap + (domain.name -> (System.currentTimeMillis(), domain))            
    domains = domains + (domain.extension -> updatedMap)
    updatedMap
  }
  
  def removeDomain(extension: String, name: String): Unit = 
    domains.get(extension) match {
      case Some(storedMap) => 
        if(storedMap.contains(name)) {
          val updatedMap = storedMap - name
          domains = 
            if(updatedMap.size == 0) domains - extension
            else domains + (extension -> updatedMap)
        }
      case _ => Unit
    }
  
  def removeDomain(parts: List[String]): Unit = {
    val trimmedParts = if(parts.reverse.head == "") parts.take(parts.size - 1) else parts
    val extension = trimmedParts.reverse.head
    val name = trimmedParts.take(parts.size - 1).mkString(".")
    removeDomain(extension, name)
  }
  
  def logDomains = logger.debug(domains.toString)
  
  def getDomains = domains
  
  def getDomainNames = domains.map { case(extension, domains) =>
    domains.map { case(name, domain) =>
      name + "." + extension
    }
  }.flatten.toArray
  
  /*def findDomain(extension: String, name: String): Option[ExtendedDomain] = 
    domains.get(extension) match {
      case None => None
      case Some(storedMap) => 
    	storedMap.get(name) match {
    	  case None => None
    	  case Some((timestamp, domain)) => {
    		val diff = timestamp + domain.ttl * 1000 - System.currentTimeMillis() //Time inserted + TTL in milliseconds - Current time
    		if(diff > 0) Some(domain) 
    		else None			  
    	  }
        }
    }*/
}

class DomainNotFoundException extends Exception