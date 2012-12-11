/**
 * *****************************************************************************
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
 * ****************************************************************************
 */
package records
import org.jboss.netty.buffer.ChannelBuffer
import org.slf4j.LoggerFactory

case class AAAA(record: Array[Byte]) extends AbstractRecord {

  val description = "AAAA"
  lazy val address = ipToString(record)
  
  def toByteArray = record

  def isEqualTo(any: Any) = any match {
    case r: AAAA => r.record == record
    case _ => false
  }
  
  def toCompressedByteArray(input: (Array[Byte], Map[String, Int])) = (input._1 ++ toByteArray, input._2)
  
  private def ipToString(bytes: Array[Byte]) = {
    def toHex(bytes: Array[Byte], hex: Array[String]): Array[String] =
      if(bytes.isEmpty) hex else toHex(bytes.takeRight(bytes.length - 2), hex :+ bytesToHex(bytes.take(2)))
      
    def indexOfZeroes(ipbase: Array[String], pos: Int = 0, index: Int = -1, curindex: Int = -1, count: Int = 0, curcount: Int = 0): (Int, Int) =
      if(ipbase.isEmpty && curcount > count) (curindex, curcount)
      else if(ipbase.isEmpty) (index, count)
      else if(ipbase.head == "0" && curindex < 0) indexOfZeroes(ipbase.tail, pos + 1, index, pos, count, curcount + 1)
      else if(ipbase.head == "0") indexOfZeroes(ipbase.tail, pos + 1, index, curindex, count, curcount + 1)
      else if(curcount > count) indexOfZeroes(ipbase.tail, pos + 1, curindex, -1, curcount, 0)
      else indexOfZeroes(ipbase.tail, pos + 1, index, -1, count, 0)
    
    val ipbase = toHex(bytes, Array[String]()).map{hex =>
      val simplified = simplifyHexString(hex)
      if(simplified == "") "0" else simplified
    }
    
    val (index, length) = indexOfZeroes(ipbase)
    val (left, right) = ipbase.splitAt(index)
    
    left.mkString(":") + "::" + right.takeRight(right.length - length).mkString(":")
  }
  
  private def bytesToHex(bytes: Array[Byte], hexstr: String = ""): String =
    bytes.map(byte => String.format("%02x", java.lang.Byte.valueOf(byte))).mkString
    
  private def simplifyHexString(str: String): String =
    if(!str.startsWith("0")) str else simplifyHexString(str.tail)
}

object AAAA {

  val logger = LoggerFactory.getLogger("app")

  def apply(buf: ChannelBuffer, recordclass: Int, size: Int) = {
    val record = recordclass match {
      // IN
      case 1 => {
        val marray = new Array[Byte](16) // A 128 bit IPv6 address = network byte order (high-order byte first).
        buf.readBytes(marray);
        marray
      }
      // *
      case 255 => null // not implemented yet
      case _ => throw new Error("Unknown record type")
    }
    new AAAA(record)
  }
}