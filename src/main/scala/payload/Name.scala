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
package payload
import org.jboss.netty.buffer.ChannelBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.List
import scala.collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import scala.annotation.tailrec

object Name {

  val logger = LoggerFactory.getLogger("app")

  /**
   * 4.1.4. Message compression
   */
  val MASK_POINTER = 0xC0; // 1100 0000

  /**
   * 2.3.4. Size limits
   */
  val MAX_LABEL_SIZE = 63; // 0011 1111

  /**
   * 2.3.4. Size limits
   */
  val MAX_NAME_SIZE = 255;

  // we should rewrite this in a more functional way
  
  def parse(buf: ChannelBuffer) = {
    @tailrec
    def loop(namesize: Int, length: Short, jumped: Boolean, list: List[Array[Byte]]): List[Array[Byte]] = {
      if (length <= 0 || buf.readableBytes < 1) {
        if(jumped) buf.resetReaderIndex
        Array[Byte]() :: list
        
      } else if ((length & MASK_POINTER) != 0) {
        val p = ((length ^ MASK_POINTER) << 8) + buf.readUnsignedByte
        if (!jumped) buf.markReaderIndex
        buf.readerIndex(p)
        loop(namesize, buf.readUnsignedByte, true, list)
        
      } else if (length <= MAX_LABEL_SIZE) {
        val ns = namesize + length
        if (MAX_NAME_SIZE < ns) {
          throw new Error("name must be 255 or less")
        }
        val marray = new Array[Byte](length)
        buf.readBytes(marray)
        loop(ns, buf.readUnsignedByte, jumped, marray :: list)
        
      } else {
        throw new Error("compression is wrong")
      }
    }
    
    loop(0, buf.readUnsignedByte, false, Nil).reverse
  }
  // TODO: VALIDATE!!!
  def toByteArray(name: List[Array[Byte]]): Array[Byte] = 
    name.foldRight(Array[Byte]()) {case(bytes, total) => 
      RRData.shortToByte((bytes.length & MAX_LABEL_SIZE).toShort) ++ bytes ++ total}
  
  def toByteArray(name: String): Array[Byte] = 
    toByteArray(name.split("""\.""").map(_.getBytes).toList)
    
}
