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
package payload

import scala.collection.immutable.BitSet
import org.jboss.netty.buffer.ChannelBuffer

class Header(buf: ChannelBuffer) {
  
  
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
  
	
	// TODO: VERIFY that the bits are 16 !!
	
	  
	  val id = buf.readUnsignedShort
	  val flagsInt = buf.readUnsignedShort
	  
	  // FLAGS
	  val qr = shiftBits(flagsInt, FLAGS_QR, 0x1) != 0 // boolean
	  val opcode = shiftBits(flagsInt, FLAGS_OPCODE, 0xF) // int
	  val aa = shiftBits(flagsInt, FLAGS_AA, 0x1) != 0 // boolean
	  val tc = shiftBits(flagsInt, FLAGS_TC, 0x1) != 0 // boolean
	  val rd = shiftBits(flagsInt, FLAGS_RD, 0x1) != 0 // boolean
	  val ra = shiftBits(flagsInt, FLAGS_RA, 0x1) != 0 // boolean
	  val z = shiftBits(flagsInt,FLAGS_Z,0x7) // always zero
	  val rcode = shiftBits(flagsInt, FLAGS_RCODE, 0xF) // int
	  
	  //-----
	  
	  
	  
	  val qdcount = buf.readUnsignedShort
	  val ancount = buf.readUnsignedShort
	  val nscount = buf.readUnsignedShort
	  val arcount = buf.readUnsignedShort	  
	  
	  
	
	
	def shiftBits(n: Int,shift: Int, mask: Int) = {
	  (n >> shift) & mask
	}

}
