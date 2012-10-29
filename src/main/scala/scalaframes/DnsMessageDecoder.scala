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


class DnsMessageDecoder extends FrameDecoder {
 
  //@Override
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buf: ChannelBuffer): payload.Message = {
    
    // 12 it is the minimum lenght in bytes of the header
    if (buf.readableBytes() < 12) {
	        // The length field was not received yet - return null.
	        // This method will be invoked again when more packets are
	        // received and appended to the buffer.
	        return null;
	   }
    
	  // The length field is in the buffer.

     // Mark the current buffer position before reading the length field
     // because the whole frame might not be in the buffer yet.
     // We will reset the buffer position to the marked position if
     // there's not enough bytes in the buffer.
     //buf.markReaderIndex();

     // Read the length field.
     //val length = buf.readUnsigned
     //println(buf.readableBytes())
          
     val message = new payload.Message(buf)
     message
     
  }
     
  
  
  
  
  
  
  

  
  
  
  
  
  def fromBytesToString(buf: ChannelBuffer, length: Int) = {
    val marray = new Array[Byte](length)
	buf.readBytes(marray)
	new String(marray, "UTF-8")
  }
  
  def toBitArracy(byte: Int, size: Short): Array[Short] = {
    var b = byte
    val bits = new Array[Short](size)
    for(i <- 0 to size){      
      val mod = b % 2
      bits(i) = mod.toShort
      b >>= 1 
    }
    bits
  }
  
  // there is probably a better way via bit operations to calculate this.
  def toInt(bits: Array[Short]): Int = {
    var n = 0
    val limit = bits.length
    for(i <- 0 to limit) {
      n = n + bits(i) * (scala.math.pow(2, i)).toInt
    }    
    n    
  }
  
  
  
  
  
  def bufferMarshall(buf: ChannelBuffer, marshall: Int): Any = {
    
	  if (buf.readableBytes() < marshall) {
	        // The length field was not received yet - return null.
	        // This method will be invoked again when more packets are
	        // received and appended to the buffer.
	        return null;
	   }
    
	  // The length field is in the buffer.

     // Mark the current buffer position before reading the length field
     // because the whole frame might not be in the buffer yet.
     // We will reset the buffer position to the marked position if
     // there's not enough bytes in the buffer.
     buf.markReaderIndex();

     // Read the length field.
     //val length = buf.readUnsigned
     println(buf.readableBytes())
     

//     // Make sure if there's enough bytes in the buffer.
//     if (buf.readableBytes() < length) {
////        // The whole bytes were not received yet - return null.
////        // This method will be invoked again when more packets are
////        // received and appended to the buffer.
////
////        // Reset to the marked position to read the length field again
////        // next time.
//    	 buf.resetReaderIndex();
////
//    	 return null;
//     }
	     
	 buf.readUnsignedShort()  	      
  } 
  
}