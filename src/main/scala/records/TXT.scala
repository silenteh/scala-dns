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
import scala.collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import scala.annotation.tailrec

case class TXT(strings: Array[Array[Byte]]) extends AbstractRecord {

  def this(strings: Array[String]) = this(strings.map(_.getBytes))
  
  val logger = LoggerFactory.getLogger("app")

  val description = "TXT"
  
  lazy val getStrings = strings.map(new String(_, "UTF-8"))
    
  def toByteArray = strings.foldRight(Array[Byte]()) {case (bytes, total) => bytes ++ total}
  
  def isEqualTo(any: Any) = any match {
    case r: TXT => r.strings.deep == strings.deep
    case _ => false
  }
  
  def toCompressedByteArray(input: (Array[Byte], Map[String, Int])) = (input._1 ++ toByteArray, input._2)
}

object TXT {

  val logger = LoggerFactory.getLogger("app")

  def apply(buf: ChannelBuffer, recordclass: Int, size: Int) = {
    val part = buf.readSlice(size)
    val strings = readPart(part, Array[Array[Byte]]())
    new TXT(strings)
  }

  @tailrec
  private def readPart(buf: ChannelBuffer, strings: Array[Array[Byte]]): Array[Array[Byte]] =
    if (!buf.readable) strings
    else {
      val length = buf.readUnsignedByte
      // throw exception string must be MAX 255
      if (AbstractRecord.MAX_STRING_LENGTH < length)
        throw new Error("String length exceeds 255")
      val marray = new Array[Byte](length)
      buf.readBytes(marray);
      readPart(buf, strings :+ marray)
    }
}
