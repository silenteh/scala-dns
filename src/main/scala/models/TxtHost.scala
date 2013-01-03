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

import records.TXT
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(Array("typ"))
case class TxtHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null, 
  @JsonProperty("value") strings: Array[String]
) extends Host("TXT") {

  def setName(newname: String) = TxtHost(cls, newname, strings)
  
  override def equals(other: Any) = other match {
    case h: TxtHost => h.cls == cls && h.name == name && h.strings.forall(str => strings.exists(_ == str))
    case _ => false
  }
  
  def getRData = new TXT(strings.map(_.getBytes))

}