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
import payload.Name
import org.slf4j.LoggerFactory
import payload.RRData

case class SOA(
  mname: List[Array[Byte]], 
  rname: List[Array[Byte]],
  serial: Long,
  refresh: Long,
  retry: Long,
  expire: Long,
  minimum: Long
) extends AbstractRecord {
  
  val description = "SOA"
  
  def toByteArray = Name.toByteArray(mname) ++ Name.toByteArray(rname) ++ RRData.intToBytes(serial.toInt) ++
    RRData.intToBytes(refresh.toInt) ++ RRData.intToBytes(retry.toInt) ++ RRData.intToBytes(expire.toInt) ++
    RRData.intToBytes(minimum.toInt)
}

object SOA {

  val logger = LoggerFactory.getLogger("app")

  def apply(buf: ChannelBuffer, recordclass: Int, size: Int) = {
    val mname = Name.parse(buf)
    val rname = Name.parse(buf)
    val serial = buf.readUnsignedInt
    val refresh = buf.readUnsignedInt
    val retry = buf.readUnsignedInt
    val expire = buf.readUnsignedInt
    val minimum = buf.readUnsignedInt
    new SOA(mname, rname, serial, refresh, retry, expire, minimum)
  }
}

