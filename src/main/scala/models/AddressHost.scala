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

//import org.codehaus.jackson.annotate.JsonProperty
import records.A
import com.fasterxml.jackson.annotation.JsonProperty

case class AddressHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null, 
  @JsonProperty("value") ips: Array[WeightedIP] = null
) extends Host("A") {
  private def ipToLong(ip: String) = ip.split("""\.""").reverse.foldRight(0L){case(part, total) => (total << 8) + part.toLong}
  protected def getRData = 
    if(ips.size == 1) ips(0).weightIP.map(ip => new A(ipToLong(ip))) 
    else ips.map(wip => wip.weightIP.map(ip => new A(ipToLong(ip)))).flatten
}

case class WeightedIP(
  @JsonProperty("weight") weight: Int = 1,
  @JsonProperty("ip") ip: String = null
) {
  def weightIP = 
    if(weight < 1) Array[String]() else Array.tabulate(weight) {i => ip}
}