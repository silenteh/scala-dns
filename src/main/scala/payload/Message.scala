package payload
import org.jboss.netty.buffer.ChannelBuffer

// This class reassemble the network frames
class Message(buf: ChannelBuffer) {
  
	/**
	 * 4.1.4. Message compression
	 */
	lazy val MASK_POINTER = 0xC0 // 1100 0000

	/**
	 * 2.3.4. Size limits
	 */
	lazy val MAX_LABEL_SIZE = 63 // 0011 1111

	/**
	 * 2.3.4. Size limits
	 */
	lazy val MAX_NAME_SIZE = 255;
  	
	
	{
	  val header = new Header(buf)
	  
	  
	  
	  println(header.id)
	  println(header.opcode)
	  
	  
	  
	  
	}
  
  
  
}