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
package records
import org.jboss.netty.buffer.ChannelBuffer
import scala.collection.mutable.ArrayBuffer

class TXT(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
  
  
		val description = "TXT"
		val strings = new ArrayBuffer[Array[Byte]]()
		val part = buf.readSlice(size)
		while (part.readable()) {
			this.strings += readString(part)
		}

		
		def readString(buf: ChannelBuffer) = {
		  val length = buf.readUnsignedByte
		  if (MAX_STRING_LENGTH < length) {
			// throw exception string must be MAX 255
		  }
		  val marray = new Array[Byte](length) 
		  buf.readBytes(marray);
		  marray
		}
		
}
