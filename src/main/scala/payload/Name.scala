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
  
  def parse(buf: ChannelBuffer, offset: Int = 0) = {
    @tailrec
    def loop(namesize: Int, length: Short, jumped: Boolean, list: List[Array[Byte]]): List[Array[Byte]] = {
      if (length <= 0 || buf.readableBytes < 1) {
        if(jumped) buf.resetReaderIndex
        Array[Byte]() :: list
        
      } else if ((length & MASK_POINTER) != 0) {
        val p = ((length ^ MASK_POINTER) << 8) + buf.readUnsignedByte + offset
        //val p = length ^ MASK_POINTER
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

  def toByteArray(name: List[Array[Byte]]): Array[Byte] = 
    name.filterNot(_.deep == Array(0).deep).foldRight(Array[Byte]()) {case(bytes, total) => 
      RRData.shortToByte((bytes.length & MAX_LABEL_SIZE).toShort) ++ bytes ++ total}
  
  /*def toByteArray(name: List[Array[Byte]]): Array[Byte] = {
    logger.debug(name.filterNot(_.deep == Array(0).deep).map(_.toList).toString)
    def namePartToBytes(name: List[Array[Byte]], bytes: Array[Byte]): Array[Byte] = {
      if(name.isEmpty) bytes
      //else if(name.head.deep == Array(RRData.shortToByte(0)).deep) bytes
      else if(name.head.isEmpty) bytes ++ RRData.shortToByte(0)
      else namePartToBytes(name.tail, bytes ++ RRData.shortToByte((name.head.length & MAX_LABEL_SIZE).toShort) ++ name.head)
    }
    namePartToBytes(name.filterNot(_.deep == Array(0).deep), Array())
  }*/
  
  def toByteArray(name: String): Array[Byte] = {
    toByteArray(name.split("""\.""").map(_.getBytes).toList)
  }
    
  def toCompressedByteArray(
    name: List[Array[Byte]], 
    input: (Array[Byte], Map[String, Int])
  ): (Array[Byte], Map[String, Int]) = {
    val (bytes, names) = input
    
    @tailrec
    def checkExisting(name: List[Array[Byte]]): List[Array[Byte]] = 
      if(name.isEmpty) Nil
      else if(names.contains(nameToString(name))) name
      else checkExisting(name.tail)
    
    val existingName = checkExisting(name)
      
    @tailrec
    def storeNewName(name: List[Array[Byte]], offset: Int = 0, map: Map[String, Int] = Map()): Map[String, Int] = 
      if(name.isEmpty || name.head.isEmpty) map 
      else storeNewName(name.tail, offset + name.head.length + 1, map + (nameToString(name ::: existingName) -> (bytes.length + offset)))

    val newName = if(existingName.isEmpty) name else name.take(name.indexOfSlice(existingName))
    
    val trailer = if(existingName.isEmpty) Array[Byte]() else RRData.shortToBytes((names(nameToString(existingName)) + (MASK_POINTER << 8)).toShort)
    (bytes ++ toByteArray(newName) ++ trailer, if(newName.isEmpty) names else names ++ storeNewName(newName))
  }
  
  def nameToString(name: List[Array[Byte]]) = name.map(new String(_, "UTF-8")).mkString(".")
}
