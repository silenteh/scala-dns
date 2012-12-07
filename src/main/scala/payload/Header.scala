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

import scala.collection.immutable.BitSet
import org.jboss.netty.buffer.ChannelBuffer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

case class Header(
  val id: Int,
  val response: Boolean,
  val opcode: Int,
  val authoritative: Boolean,
  val truncated: Boolean,
  val recursionDesired: Boolean,
  val recursionAvailable: Boolean,
  val zero: Int,
  val rcode: Int,
  val questionCount: Int,
  val answerCount: Int,
  val authorityCount: Int,
  val additionalCount: Int) {
  def toByteArray = {
    getBytes(
      (id << 16) + (boolToInt(response) << 15) + (opcode << 11) + (boolToInt(authoritative) << 10) + (boolToInt(truncated) << 9) +
      (boolToInt(recursionDesired) << 8) + (boolToInt(recursionAvailable) << 6) + (zero << 4) + rcode) ++ 
      getBytes((questionCount << 16) + answerCount) ++ getBytes((authorityCount << 16) + additionalCount)
  }

  def getBytes(number: Int): Array[Byte] = {
    val buffer = ByteBuffer.allocate(4)
    buffer.putInt(number)
    buffer.array
  }

  def boolToInt(bool: Boolean) = if (bool) 1.toShort else 0.toShort
  
  def toCompressedByteArray(input: (Array[Byte], Map[String, Int])) = (input._1 ++ toByteArray, input._2)
}

object Header {
  val logger = LoggerFactory.getLogger("app")

  lazy val MIN_USHORT = 0;
  lazy val MAX_USHORT = 0xFFFF;
  lazy val FLAGS_QR = 15;
  lazy val FLAGS_OPCODE = 11;
  lazy val FLAGS_AA = 10;
  lazy val FLAGS_TC = 9;
  lazy val FLAGS_RD = 8;
  lazy val FLAGS_RA = 7;
  lazy val FLAGS_Z = 4;
  lazy val FLAGS_RCODE = 0;

  object OP_CODE extends Enumeration {
    val QUERY = 0
    val IQUERY = 1
    val STATUS = 2
  }

  def apply(buf: ChannelBuffer) = {
    // TODO: VERIFY that the bits are 16 !!
    val id = buf.readUnsignedShort
    val flagsInt = buf.readUnsignedShort

    new Header(
      id,
      shiftBits(flagsInt, FLAGS_QR, 0x1) != 0, // boolean
      shiftBits(flagsInt, FLAGS_OPCODE, 0xF), // int
      shiftBits(flagsInt, FLAGS_AA, 0x1) != 0, // boolean
      shiftBits(flagsInt, FLAGS_TC, 0x1) != 0, // boolean
      shiftBits(flagsInt, FLAGS_RD, 0x1) != 0, // booelan
      shiftBits(flagsInt, FLAGS_RA, 0x1) != 0, // boolean
      shiftBits(flagsInt, FLAGS_Z, 0x7), // always zero
      shiftBits(flagsInt, FLAGS_RCODE, 0xF), // int
      buf.readUnsignedShort, // qdcount
      buf.readUnsignedShort, // ancount
      buf.readUnsignedShort, // nscount
      buf.readUnsignedShort // arcount
      )
  }

  def shiftBits(n: Int, shift: Int, mask: Int) =
    (n >> shift) & mask
}