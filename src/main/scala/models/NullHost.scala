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

import payload.RRData
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import records.NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import utils.HostnameUtils

@JsonIgnoreProperties(Array("typ"))
case class NullHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null,
  @JsonProperty("value") value: Any
) extends Host("NULL") {

  def setName(newname: String) = NullHost(cls, newname, value)
  
  def getRData = new NULL(valueByteArray)
  
  override def toAbsoluteNames(domain: ExtendedDomain) = 
    NullHost(cls, HostnameUtils.absoluteHostName(name, domain.fullName), value)
  
  override def equals(other: Any) = other match {
    case h: NullHost => h.cls == cls && h.name == name && h.value.equals(value)
    case _ => false
  }  
  
  lazy val valueByteArray = value match {
    case v: String => v.getBytes
    case v: Int => RRData.intToBytes(v)
    case v: Array[String] => v.map(_.getBytes).flatten
    case v: Array[Int] => v.map(RRData.intToBytes(_)).flatten
    case _ => {
      val bos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bos)
      oos.writeObject(value)
      
      val bytes = bos.toByteArray
      oos.close
      bos.close
      
      bytes
    }
  }

}