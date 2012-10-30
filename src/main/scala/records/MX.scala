package records
import org.jboss.netty.buffer.ChannelBuffer
import payload.Name

class MX(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
    
  val preference = buf.readUnsignedShort
  val record = Name.parse(buf)
  

}