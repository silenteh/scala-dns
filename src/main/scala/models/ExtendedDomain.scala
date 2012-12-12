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
package models

import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.annotate.JsonManagedReference
import enums.RecordType

case class ExtendedDomain(
  @JsonProperty("origin") fullName: String,
  @JsonProperty("ttl") ttl: Long,
  @JsonProperty("NS") nameservers: Array[NSHost] = Array(),
  @JsonProperty("SOA") settings: Array[SoaHost] = null,
  @JsonProperty("CNAME") cname: Array[CnameHost] = null,
  @JsonProperty("A") address: Array[AddressHost] = null,
  @JsonProperty("AAAA") ipv6address: Array[IPv6AddressHost] = null,
  @JsonProperty("MX") mailx: Array[MXHost] = null,
  @JsonProperty("HSS") otherhosts: Array[GenericHost] = null
) extends AbstractDomain {
  lazy val hosts: List[Host] =
    hostsToList(nameservers) ++ hostsToList(cname) ++ hostsToList(address) ++ hostsToList(ipv6address) ++ 
    hostsToList(mailx) ++ hostsToList(otherhosts)
    
  def hasRootEntry(typ: Int = 0) = 
    findHost(fullName, typ) != None || findHost("@", typ) != None || findHost(fullName, 5) != None || findHost("@", 5) != None

  def findHost(name: String = null, typ: Int = 0) = 
    RecordType(typ).toString match {
      case "A" => findInArrayWithNull(address, compareHostName(name))
      case "AAAA" => findInArrayWithNull(ipv6address, compareHostName(name))
      case "CNAME" => findInArrayWithNull(cname, compareHostName(name))
      case "NS" => findInArrayWithNull(nameservers, compareHostName(name))
      case "SOA" => findInArrayWithNull(settings, compareHostName(name))
      case "PTR" => None
      case "TXT" => None
      case "MX" => if(mailx != null) {
        val mx = mailx.filter(compareHostName(name)(_))
        if(mx.isEmpty) None else Some(mx.minBy(_.priority))
      } else None
      case _ => None
    }
  
  def getHost(name: String = null, typ: Int = 0) =
    findHost(name, typ) match {
      case Some(host) => host
      case None => throw new HostNotFoundException
    }
  
  def findHosts(name: String) = 
    hosts.filter(compareHostName(name)(_))
  
  def getHosts(name: String) = { 
    val hosts = findHosts(name)
    //if(!hosts.isEmpty) hosts else throw new HostNotFoundException
    hosts
  }
  
  private def compareHostName(name: String)(host: Host) = 
    host.name == name || (name == fullName && (host.name == fullName || host.name == "@"))
    
  private def hostsToList[T <: Host](hosts: Array[T]): List[Host] =
    if (hosts != null) hosts.toList else Nil
    
  private def findInArrayWithNull[T <: Host](array: Array[T], condition: T => Boolean) = 
    if(array != null) array.find(condition) else None
}

class HostNotFoundException extends Exception