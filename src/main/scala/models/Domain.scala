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

import org.slf4j.LoggerFactory
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnore

/*class Domain(val extension: String, val name: String, val ttl: Long, val nameservers: List[Host] = List.empty[Host]) {
  
  def fullName() = {
    val fn = name + "." + extension
    fn
  }
  
  def reverseFullName() = {
    val fn = extension + "." + name
    fn
  }
  
  

}*/

abstract class AbstractDomain {
  @JsonIgnore val fullName: String
  val ttl: Long
  val nameservers: Array[NSHost]
  
  @JsonIgnore lazy val nameParts = fullName.split("""\.""")
  @JsonIgnore lazy val extension = nameParts(nameParts.size - 1)
  @JsonIgnore lazy val name = if(nameParts.size > 1) nameParts.take(nameParts.size - 1).mkString(".") else ""
  @JsonIgnore lazy val reverseFullName = fullName.split(".").reverse.mkString(".")
}

case class Domain(
  @JsonProperty("origin") val fullName: String = null, 
  @JsonProperty("ttl") val ttl: Long = -1, 
  @JsonProperty("ns") val nameservers: Array[NSHost] = Array()
) extends AbstractDomain