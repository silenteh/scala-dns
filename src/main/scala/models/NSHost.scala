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

import records.NS
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(Array("typ"))
case class NSHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null,
  @JsonProperty("value") hostnames: Array[WeightedNS] = null
) extends Host("NS") {
  def setName(newname: String) = NSHost(cls, newname, hostnames)
  
  override def equals(other: Any) = other match {
    case h: NSHost => cls == h.cls && name == h.name && h.hostnames.forall(wns => hostnames.exists(_.hostname == wns.hostname))
    case _ => false
  }
  
  //protected def getRData2 = new NS((hostname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList)
  
  protected def getRData = 
    if(hostnames.size == 1) hostnames(0).weightNS.map(hostname => new NS((hostname.split("""\.""").map(_.getBytes) :+ Array(0.toByte)).toList))
    else hostnames.map(hostname => hostname.weightNS.map(wns =>  new NS((wns.split("""\.""").map(_.getBytes) :+ Array(0.toByte)).toList))).flatten
}

case class WeightedNS(
  @JsonProperty("weight") weight: Int = 1,
  @JsonProperty("ns") hostname: String = null
) {
  def weightNS = 
    if(weight < 1) Array[String]() else Array.tabulate(weight) {i => hostname}
}