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

import scala.annotation.tailrec
import records.SOA
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(Array("typ"))
case class SoaHost(
  @JsonProperty("class") cls: String = null,
  @JsonProperty("name") name: String = null,
  @JsonProperty("mname") mname: String = null,
  @JsonProperty("rname") rname: String = null,
  @JsonProperty("at") ttl: String = null,
  @JsonProperty("serial") serial: String = null,
  @JsonProperty("refresh") refresh: String = null,
  @JsonProperty("retry") retry: String = null,
  @JsonProperty("expire") expire: String = null,
  @JsonProperty("minimum") minimum: String = null
) extends Host("SOA") {
  
  def setName(newname: String) = SoaHost(cls, newname, mname, rname, ttl, serial, refresh, retry, expire, minimum)
  
  def updateSerial(serial: String) = SoaHost(cls, name, mname, rname, ttl, serial, refresh, retry, expire, minimum)
  
  override def equals(other: Any) = other match {
    case h: SoaHost => h.cls == cls && h.name == name && h.mname == mname && h.rname == rname && h.ttl == ttl && 
      h.serial == serial && h.refresh == refresh && h.retry == retry && h.expire == expire && h.minimum == minimum
    case _ => false
  }
  
  @JsonIgnore
  private lazy val timeStrs = Map("S" -> 1L, "M" -> 60L, "H" -> 3600L, "D" -> 86400L).withDefaultValue(0L)
  
  @tailrec
  @JsonIgnore
  private def timeStrToLong(time: String, number: String = "", value: Long = 0L): Long =
    if(time.isEmpty) value + (if(number != "") number.toLong else 0L)
    else if(time.head.toString.matches("[0-9]")) timeStrToLong(time.tail, number + time.head, value)
    else timeStrToLong(time.tail, "", value + strNumToLong(number) * timeStrs(time.head.toString))
  
  @JsonIgnore
  private def strNumToLong(number: String): Long = 
    if(number == "") 0L else number.toLong
  
  @JsonIgnore
  protected def getRData = 
    new SOA((mname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList, (rname.split(".").map(_.getBytes) :+ Array(0.toByte)).toList,
      serial.toLong, timeStrToLong(refresh), timeStrToLong(retry), timeStrToLong(expire), timeStrToLong(minimum))
}