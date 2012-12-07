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
import records._
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

case class RRData(
  name: List[Array[Byte]],
  rtype: Int,
  rclass: Int,
  ttl: Long,
  rdlength: Int,
  rdata: AbstractRecord
) {
  def toByteArray = {
    Name.toByteArray(name) ++ RRData.shortToBytes(rtype.toShort) ++ 
    RRData.shortToBytes(rclass.toShort) ++ RRData.intToBytes(ttl.toInt) ++ RRData.shortToBytes(rdlength.toShort) ++ rdata.toByteArray
  }
  
  def toCompressedByteArray(input: (Array[Byte], Map[String, Int])) = {
    val nameBytes = Name.toCompressedByteArray(name, input)
    val rrHeaderBytes = (nameBytes._1 ++ RRData.shortToBytes(rtype.toShort) ++ RRData.shortToBytes(rclass.toShort) ++ 
      RRData.intToBytes(ttl.toInt) ++ RRData.shortToBytes(rdlength.toShort), nameBytes._2)
    rdata.toCompressedByteArray(rrHeaderBytes)
  }
  
  override def toString = 
    name.map(new String(_, "UTF-8")).mkString(".") + "," +
    rtype + "," + rclass + "," + ttl + "," + rdlength + "," + rdata.toString
    
}

object RRData {

  val logger = LoggerFactory.getLogger("app")

  def apply(buf: ChannelBuffer) = {
    val name = Name.parse(buf)
    val rtype = buf.readUnsignedShort
    val rclass = buf.readUnsignedShort
    val ttl = buf.readUnsignedInt()
    val rdlength = buf.readUnsignedShort
    val rdata = deserializeRecord(buf, rtype, rclass, rdlength)
    new RRData(name, rtype, rclass, ttl, rdlength, rdata)
  }

  private def deserializeRecord(buf: ChannelBuffer, recordtype: Int, recordclass: Int, size: Int) = 
    recordtype match {
      // A 
      case 1 => A(buf, recordclass, size)
      // NS
      case 2 => NS(buf, recordclass, size)
      // MD
      case 3 => null // NYI
      // MF
      case 4 => null // NYI
      // CNAME
      case 5 => CNAME(buf, recordclass, size)
      // SOA
      case 6 => SOA(buf, recordclass, size)
      // MB
      case 7 => null
      // MG
      case 8 => null
      // MR  
      case 9 => null
      // NULL
      case 10 => NULL(buf, recordclass, size)
      // WKS
      case 11 => null
      // PTR
      case 12 => PTR(buf, recordclass, size)
      //HINFO
      case 13 => null
      // MINFO
      case 14 => null
      // MX
      case 15 => MX(buf, recordclass, size)
      // TXT
      case 16 => TXT(buf, recordclass, size)

      case 28 => AAAA(buf, recordclass, size)
      // AXFR
      case 252 => null
      // *
      case 255 => null
    }
  
    def intToBytes(number: Int) = {
      val buffer = ByteBuffer.allocate(4)
      buffer.putInt(number)
      buffer.array
    }
    
    def longToBytes(number: Long) = {
      val buffer = ByteBuffer.allocate(8)
      buffer.putLong(number)
      buffer.array
    }
    
    def shortToBytes(number: Short) = {
      val buffer = ByteBuffer.allocate(4)
      buffer.putInt(number)
      buffer.array.takeRight(2)
    }
    
    def shortToByte(number: Short) = {
      val buffer = ByteBuffer.allocate(4)
      buffer.putInt(number)
      buffer.array.takeRight(1)
    }

  //  private def deserializeBasedOnClass(buf: ChannelBuffer, rrecordclass: Int, size: Int) = {
  //    
  //    rrecordclass match {
  //    	case 1 =>
  //    	  
  //    	case 2 =>
  //    	  
  //    	case 3 =>
  //    	  
  //    	case 4 =>
  //    	  
  //    	case 255 =>
  //    }
  //    
  //  }

  // NAME            a domain name to which this resource record pertains.
  //var name = ""

  // TYPE         two octets containing one of the RR type codes.  This
  //              field specifies the meaning of the datastructures in the RDATA
  //              field.  
  //var rtype= ""

  // CLASS        two octets which specify the class of the datastructures in the
  //              RDATA field.
  //var rclass = ""

  // TTL          a 32 bit unsigned integer that specifies the time
  //              interval (in seconds) that the resource record may be
  //              cached before it should be discarded.  Zero values are
  //              interpreted to mean that the RR can only be used for the
  //              transaction in progress, and should not be cached.
  //var ttl = 0

  // RDLENGTH     an unsigned 16 bit integer that specifies the length in
  //              octets of the RDATA field.
  //var rdlenght = 0

  // RDATA        a variable length string of octets that describes the
  //              resource.  The format of this information varies
  //              according to the TYPE and CLASS of the resource record.
  //              For example, the if the TYPE is A and the CLASS is IN,
  //              the RDATA field is a 4 octet ARPA Internet address.
  //var rdata = ""
}
