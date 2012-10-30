package records
import org.jboss.netty.buffer.ChannelBuffer
import scala.collection.mutable.ArrayBuffer

class TXT(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
  
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