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

import records.PTR
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import utils.HostnameUtils

@JsonIgnoreProperties(Array("typ"))
case class PointerHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null, 
  @JsonProperty("value") ptrdname: String
) extends Host("PTR") {

  def setName(newname: String) = PointerHost(cls, newname, ptrdname)
  
  override def toAbsoluteNames(domain: ExtendedDomain) = 
    PointerHost(cls, HostnameUtils.absoluteHostName(name, domain.fullName), HostnameUtils.absoluteHostName(ptrdname, domain.fullName))
  
  override def equals(other: Any) = other match {
    case h: PointerHost => h.cls == cls && h.name == name && h.ptrdname == ptrdname
    case _ => false
  }
  
  protected def getRData = new PTR((ptrdname.split("""\.""").map(_.getBytes) :+ Array[Byte]()).toList)

}