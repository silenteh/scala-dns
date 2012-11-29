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
import org.codehaus.jackson.annotate.JsonIgnoreProperties
import org.codehaus.jackson.annotate.JsonIgnore
import org.codehaus.jackson.annotate.JsonBackReference
import records._
import payload.RRData

/*class Host(name: String, domain: ExtendedDomain, recordType: Int, ip: List[String] = List.empty[String])*/

abstract class Host(val typ: String) {
  val cls: String
  
  protected def getRData: Any
  
  def toRData = 
    getRData match {
      case rd: AbstractRecord => Array(rd)
      case rd: Array[AbstractRecord] => rd
      case _ => throw new Error("Something went wrong")
    }
}

case class NSHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("weight") weight: Int = 0,
  @JsonProperty("value") hostname: String = null
) extends Host("NS") {
  protected def getRData = new NS((hostname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList)
}

case class AddressHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null, 
  @JsonProperty("value") ip: Array[String] = null
) extends Host("A") {
  protected def getRData = if(ip.size == 1) new A(ipToLong(ip(0))) else ip.map(ip => new A(ipToLong(ip)))
  private def ipToLong(ip: String) = ip.split("""\.""").reverse.foldRight(0L){case(part, total) => (total << 8) + part.toLong}
}

case class MXHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("value") hostname: String = null, 
  @JsonProperty("priority") priority: Int = -1
) extends Host("MX") {
  protected def getRData = new MX(priority, (hostname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList)
}

case class CnameHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null, 
  @JsonProperty("value") hostname: String
) extends Host("CNAME") {
  protected def getRData = new CNAME((hostname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList)
}

case class Soa(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("at") ttl: String = null,
  @JsonProperty("hosts") hosts: Array[String] = null,
  @JsonProperty("serial") serial: String = null,
  @JsonProperty("refresh") refresh: String = null,
  @JsonProperty("retry") retry: String = null,
  @JsonProperty("expire") expire: String = null,
  @JsonProperty("minimum") minimum: String = null
)

case class GenericHost(
  @JsonProperty("type") override val typ: String = null,
  @JsonProperty("class") cls: String = null,
  @JsonProperty("value") value: String = ""
) extends Host(typ) {
  protected def getRData = Unit
}
