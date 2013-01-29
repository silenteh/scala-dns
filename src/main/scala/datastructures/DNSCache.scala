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

object DNSCache extends DNSDomainStorage[(Long, ExtendedDomain)] {
  
  protected var domains = TreeMap[String,Map[String, (Long, ExtendedDomain)]]("." -> Map())

  @tailrec
  protected def findDomainName(typ: Int, parts: List[String], storedMap: Map[String, (Long, ExtendedDomain)], name: Seq[String]): Option[ExtendedDomain] = 
    if(name.isEmpty) None
    else storedMap.get(name.mkString(".")) match {
      case Some((timestamp, domain)) => {
        val diff = timestamp + domain.ttl * 1000 - System.currentTimeMillis()
        if((parts.size - 1 != name.size || domain.hasRootEntry(typ)) && diff > 0) Some(domain)
        else findDomainName(typ, parts, storedMap, name.tail)
      }
      case _ => findDomainName(typ, parts, storedMap, name.tail)
    }
  
  protected def addDomainEntry(domain: ExtendedDomain) = (domain.name -> (System.currentTimeMillis(), domain))
  
}