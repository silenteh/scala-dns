package scalaframes
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.frame.FrameDecoder
import com.google.protobuf.Message
import payload.Header
import scala.collection.immutable.BitSet


class DnsMessageDecoder extends FrameDecoder {
  
  
  object HEADER { 
	  val id = 2
	  val EmptyDescription = "No Description"
  }
  
  
  object QUERY { 
	  val EmptyID = 0
	  val EmptyDescription = "No Description"
  }
  
  
  object DATA { 
	  val EmptyID = 0
	  val EmptyDescription = "No Description"
  }
  
  object STATE {
    val READING_HEADER = 0
    val READING_QUERY = 1
    val READING_ANSWER = 2
    val READING_AUTHORITY = 3
    val READING_ADDITIONAL = 4
    val FINISHED = 5
    
    
  }
  
  
  var state = STATE.READING_HEADER
  
  
  var header = new Header
  var query = null
  
  
  
  
  //@Override
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buf: ChannelBuffer): Message = {
    
    val enoughData = state match {
    	case STATE.READING_HEADER 		=> 
    	case STATE.READING_QUERY 		=>
    	case STATE.READING_ANSWER 		=>
    	case STATE.READING_AUTHORITY 	=>
    	case STATE.READING_ADDITIONAL 	=>
    	  
    }
    
	     // Make sure if the length field was received.
    
//		  buf.readableBytes() match {
//		  	case value =>
//		  	  
//		  }
		  
		  
		  
    
		 println(buf.readableBytes())
	     if (buf.readableBytes() < HEADER.id) {
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
	     val length = buf.readInt();

	     // Make sure if there's enough bytes in the buffer.
	     if (buf.readableBytes() < length) {
	        // The whole bytes were not received yet - return null.
	        // This method will be invoked again when more packets are
	        // received and appended to the buffer.

	        // Reset to the marked position to read the length field again
	        // next time.
	        buf.resetReaderIndex();

	        return null;
	     }

	     // There's enough bytes in the buffer. Read it.
	     val id = buf.readBytes(length);
	     
	     

	     // Successfully decoded a frame.  Return the decoded frame.
	     return null;
	   }
     
  
  
  
  
  def decodeHeader(buf: ChannelBuffer): Any = {
    
        
    if(header.id < 0 && bufferMarshall(buf, 2) != null) {      
    	  header.id = buf.readBytes(2).readInt();
    	  header.id
    } else if (header.qr < 0 && bufferMarshall(buf, 2) != null) {
    	val bits = toBitArracy(buf.getByte(0),8)
    	header.qr = bits(0)    	
    	header.opcode = toInt(bits.slice(1, 4))    	
    	header.aa = bits(5)
    	header.tc = bits(6)
    	header.rd = bits(7)
    	
    	header.rd = bits(8)
    	header.rcode = toInt(bits.slice(12, 16)) 
    } else if (header.qdcount < 0 && bufferMarshall(buf, 2) != null) {    	   	
        header.qdcount = buf.getUnsignedShort(0)    	
    } else if (header.ancount < 0 && bufferMarshall(buf, 2) != null) {    	   	
        header.ancount = buf.getUnsignedShort(0)    	
    } else if (header.nscount < 0 && bufferMarshall(buf, 2) != null) {    	   	
        header.nscount = buf.getUnsignedShort(0)    	
    } else if (header.arcount < 0 && bufferMarshall(buf, 2) != null) {    	   	
        header.arcount = buf.getUnsignedShort(0)    	
    }
            
    null
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
     val length = buf.readInt();

     // Make sure if there's enough bytes in the buffer.
     if (buf.readableBytes() < length) {
        // The whole bytes were not received yet - return null.
        // This method will be invoked again when more packets are
        // received and appended to the buffer.

        // Reset to the marked position to read the length field again
        // next time.
        buf.resetReaderIndex();

        return null;
     }
	     
	 buf.readBytes(length)  	      
  } 
  
}