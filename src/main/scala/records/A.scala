package records
import org.jboss.netty.buffer.ChannelBuffer

class A(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size){
  
  
  val description = "A"
	val record = recordclass match {
    	// IN
    	case 1 => buf.readUnsignedInt() //return a 32 bit Internet Address
    	// *
    	case 255 => null// not implemented yet
    }
	
	println(record)
  
}