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
  @JsonProperty("NS")@JsonManagedReference("domain-ns") nameservers: Array[NSHost] = Array(),
  @JsonProperty("SOA") settings: Array[Soa] = null,
  @JsonProperty("CNAME") cname: Array[CnameHost] = null,
  @JsonProperty("A") address: Array[AddressHost] = null,
  @JsonProperty("MX") mailx: Array[MXHost] = null,
  @JsonProperty("HSS") otherhosts: Array[GenericHost] = null
) extends AbstractDomain {
  lazy val hosts: List[Host] =
    hostsToList(nameservers) ++ hostsToList(cname) ++ hostsToList(address) ++ hostsToList(mailx) ++ hostsToList(otherhosts)

  private def hostsToList[T <: Host](hosts: Array[T]): List[Host] =
    if (hosts != null) hosts.toList else Nil

  def findHost(name: String = null, typ: Int = 0) = 
    RecordType(typ).toString match {
      case "A" =>
        address.find(host => host.name == name)
      case "AAAA" =>
        None
      case "CNAME" =>
        cname.find(host => host.name == name)
      case "MX" =>
        if (!mailx.isEmpty) Some(mailx.minBy(_.priority)) else None
      case "NS" =>
        None
      case "SOA" =>
        None
      case "PTR" =>
        None
      case "TXT" =>
        None
      case _ =>
        None
    }
  
  def getHost(name: String = null, typ: Int = 0) =
    findHost(name, typ) match {
      case Some(host) => host
      case None => throw new HostNotFoundException
    }
}

class HostNotFoundException extends Exception