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
package scalaframes
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.frame.FrameDecoder
import com.google.protobuf.Message
import payload.Header
import scala.collection.immutable.BitSet
import payload.Question
import payload.Message
import org.slf4j.LoggerFactory
import scala.annotation.tailrec

class UDPDnsMessageDecoder extends FrameDecoder {

  val logger = LoggerFactory.getLogger("app")

  //@Override
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buf: ChannelBuffer): payload.Message = {

    // 12 it is the minimum lenght in bytes of the header
    if (buf.readableBytes() < 12) null
    else {
      // The length field was not received yet - return null.
      // This method will be invoked again when more packets are
      // received and appended to the buffer.

      // The length field is in the buffer.

      // Mark the current buffer position before reading the length field
      // because the whole frame might not be in the buffer yet.
      // We will reset the buffer position to the marked position if
      // there's not enough bytes in the buffer.
      //buf.markReaderIndex();

      // Read the length field.
      //val length = buf.readUnsigned
      //println(buf.readableBytes())
      payload.Message(buf)
    }

  }

  /*def fromBytesToString(buf: ChannelBuffer, length: Int) = {
    val marray = new Array[Byte](length)
    buf.readBytes(marray)
    new String(marray, "UTF-8")
  }*/

  /*def toBitArracy(byte: Int, size: Short): Array[Short] =
    Array.tabulate(size) { i => ((byte >> (size - i - 1)) % 2).toShort }*/

  // there is probably a better way via bit operations to calculate this.
  // not a tail recursion
  //def toInt(bits: Array[Short]): Int =
  //  if (bits.isEmpty) 0 else (bits.head * (scala.math.pow(2, bits.tail.length))).toInt + toInt(bits.tail)

  def bufferMarshall(buf: ChannelBuffer, marshall: Int): Any = {

    if (buf.readableBytes() < marshall) null
    // The length field was not received yet - return null.
    // This method will be invoked again when more packets are
    // received and appended to the buffer.
    else {

      // The length field is in the buffer.

      // Mark the current buffer position before reading the length field
      // because the whole frame might not be in the buffer yet.
      // We will reset the buffer position to the marked position if
      // there's not enough bytes in the buffer.
      buf.markReaderIndex();
      buf.readUnsignedShort()
    }
  }

}
