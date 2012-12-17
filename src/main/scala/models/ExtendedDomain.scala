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

import enums.RecordType
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnore

case class ExtendedDomain(
  @JsonProperty("origin") fullName: String,
  @JsonProperty("ttl") ttl: Long,
  @JsonProperty("NS") nameservers: Array[NSHost] = Array(),
  @JsonProperty("SOA") settings: Array[SoaHost] = null,
  @JsonProperty("CNAME") cname: Array[CnameHost] = null,
  @JsonProperty("A") address: Array[AddressHost] = null,
  @JsonProperty("AAAA") ipv6address: Array[IPv6AddressHost] = null,
  @JsonProperty("PTR") pointer: Array[PointerHost] = null,
  @JsonProperty("TXT") text: Array[TxtHost] = null,
  @JsonProperty("MX") mailx: Array[MXHost] = null,
  @JsonProperty("OH") otherhosts: Array[GenericHost] = null
) extends AbstractDomain {
  @JsonIgnore
  lazy val hosts: List[Host] =
    hostsToList(nameservers) ++ hostsToList(cname) ++ hostsToList(address) ++ hostsToList(ipv6address) ++ 
    hostsToList(settings) ++ hostsToList(pointer) ++ hostsToList(text) ++ hostsToList(mailx) ++ hostsToList(otherhosts)
  
  @JsonIgnore
  def hasRootEntry(typ: Int = 0) = 
    if(typ == RecordType.ALL.id || typ == RecordType.AXFR.id) !findHosts(fullName).isEmpty || !findHosts("@").isEmpty
    else findHost(fullName, typ) != None || findHost("@", typ) != None || findHost(fullName, 5) != None || findHost("@", 5) != None

  @JsonIgnore
  def findHost(name: String = null, typ: Int = 0) = 
    RecordType(typ).toString match {
      case "A" => findInArrayWithNull(address, compareHostName(name))
      case "AAAA" => findInArrayWithNull(ipv6address, compareHostName(name))
      case "CNAME" => findInArrayWithNull(cname, compareHostName(name))
      case "NS" => findInArrayWithNull(nameservers, compareHostName(name))
      case "SOA" => findInArrayWithNull(settings, compareHostName(name))
      case "PTR" => findInArrayWithNull(pointer, compareHostName(name))
      case "TXT" => findInArrayWithNull(text, compareHostName(name))
      case "MX" => if(mailx != null) {
        val mx = mailx.filter(compareHostName(name)(_))
        if(mx.isEmpty) None else Some(mx.minBy(_.priority))
      } else None
      case _ => None
    }
  
  @JsonIgnore
  def getHost(name: String = null, typ: Int = 0) =
    findHost(name, typ) match {
      case Some(host) => host
      case None => throw new HostNotFoundException
    }
  
  @JsonIgnore
  def findHosts(name: String) = 
    hosts.filter(compareHostName(name)(_))
  
  @JsonIgnore
  def getHosts(name: String) =  
    findHosts(name)
  
  @JsonIgnore
  private def compareHostName(name: String)(host: Host) = 
    host.name == name || (name == fullName && (host.name == fullName || host.name == "@"))
  
  @JsonIgnore
  private def hostsToList[T <: Host](hosts: Array[T]): List[Host] =
    if (hosts != null) hosts.toList else Nil
  
  @JsonIgnore
  private def findInArrayWithNull[T <: Host](array: Array[T], condition: T => Boolean) = 
    if(array != null) array.find(condition) else None
  
}

class HostNotFoundException extends Exception