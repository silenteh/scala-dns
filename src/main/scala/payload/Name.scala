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
    /*val list = ArrayBuffer.empty[Array[Byte]]

    var namesize = 0
    var length = buf.readUnsignedByte
    var jumped = false

    while (0 < length) {

      if (length == 0) {
        list += Array.empty[Byte]

      } else if ((length & MASK_POINTER) != 0) {
        val p = ((length ^ MASK_POINTER) << 8) + buf.readUnsignedByte();
        if (jumped == false) {
          buf.markReaderIndex()
          jumped = true
        }
        buf.readerIndex(p);
      } else if (length <= MAX_LABEL_SIZE) {
        namesize += length;
        if (MAX_NAME_SIZE < namesize) {
          // throw an exception where the name must be 255 or less					
        }
        val marray = new Array[Byte](length)
        buf.readBytes(marray);
        list += marray
      } else {
        // throw an exception because the compression is wrong !
      }

      length = if (length == 0) -1 else buf.readUnsignedByte
    }

    if (jumped) {
      buf.resetReaderIndex();
    }

    list.toList*/
    
    def loop(namesize: Int, length: Short, jumped: Boolean, list: Vector[Array[Byte]]): Vector[Array[Byte]] =
      if (length <= 0 || buf.readableBytes < 1) {
        if(jumped) buf.resetReaderIndex
        list
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
        loop(ns, buf.readUnsignedByte, jumped, list :+ marray)
      } else {
        throw new Error("compression is wrong")
      }

    loop(0, buf.readUnsignedByte, false, Vector()).toList
  }

}
