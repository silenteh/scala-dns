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

import records._
import payload.RRData
import scala.annotation.tailrec
import com.fasterxml.jackson.annotation.JsonIgnore

/*class Host(name: String, domain: ExtendedDomain, recordType: Int, ip: List[String] = List.empty[String])*/

abstract class Host(@JsonIgnore val typ: String) {
  val cls: String
  val name: String
  
  override def equals(obj: Any): Boolean
  
  protected def getRData: Any
  
  def toAbsoluteNames(domain: ExtendedDomain): Host
  
  def setName(newname: String): Host
  
  def toRData = 
    getRData match {
      case rd: AbstractRecord => Array(rd)
      case rd: Array[AbstractRecord] => rd
      case _ => throw new Error("Something went wrong")
    }
}