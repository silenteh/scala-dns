package records
import org.jboss.netty.buffer.ChannelBuffer


class AAAA(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
  
  
  
  val record = recordclass match {
    	// IN
    	case 1 => {    		
			val marray = new Array[Byte](16) // A 128 bit IPv6 address = network byte order (high-order byte first).
			buf.readBytes(marray);
			marray
    	}
    	// *
    	case 255 => null	// not implemented yet
    }

}