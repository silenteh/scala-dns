package records
import org.jboss.netty.buffer.ChannelBuffer
import payload.Name

class NS(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
  
  val description = "NS"
  val record = recordclass match {
    	// IN
    	case 1 => Name.parse(buf)
    	// *
    	case 255 => null// not implemented yet
  }  
}