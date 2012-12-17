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

case class NSHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null,
  @JsonProperty("weight") weight: Int = 0,
  @JsonProperty("value") hostname: String = null
) extends Host("NS") {
  protected def getRData = new NS((hostname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList)
}