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
import payload.RRData

case class A(record: Long) extends AbstractRecord {

  val description = "A"
  lazy val address = RRData.intToBytes(record.toInt).map(b => if (b < 0) b + 256 else b).reverse.mkString(".")

  def toByteArray = RRData.intToBytes(record.toInt)
  def addressToByteArray = Array()
  
  def isEqualTo(any: Any) = any match {
    case r: A => r.record == record
    case _ => false
  }
  
  def toCompressedByteArray(input: (Array[Byte], Map[String, Int])) = (input._1 ++ toByteArray, input._2)

}

object A {

  val logger = LoggerFactory.getLogger("app")

  def apply(buf: ChannelBuffer, recordclass: Int, size: Int) = {
    val record = recordclass match {
      // IN
      case 1 => buf.readUnsignedInt() //return a 32 bit Internet Address
      // *
      case 255 => 0L // not implemented yet
      case _ => throw new Error("Error: Unknown address format")
    }
    new A(record)
  }
  
}
