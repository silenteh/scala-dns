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

import records.MX
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(Array("typ"))
case class MXHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null,
  @JsonProperty("value") hostname: String = null, 
  @JsonProperty("priority") priority: Int = -1
) extends Host("MX") {
  def setName(newname: String) = MXHost(cls, newname, hostname, priority)
  
  override def equals(other: Any) = other match {
    case h: MXHost => h.cls == cls && h.name == name && h.hostname == hostname && h.priority == priority
    case _ => false
  }
  
  protected def getRData = new MX(priority, (hostname.split("""\.""").map(_.getBytes) :+ Array(0.toByte)).toList)
}