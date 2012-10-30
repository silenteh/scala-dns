package records
import org.jboss.netty.buffer.ChannelBuffer
import payload.Name

class SOA(buf: ChannelBuffer, recordclass: Int, size: Int) extends AbstractRecord(buf,recordclass,size) {
    
  val mname = Name.parse(buf)
  val rname = Name.parse(buf)
  val serial = buf.readUnsignedInt
  val refresh = buf.readUnsignedInt
  val retry = buf.readUnsignedInt
  val expire = buf.readUnsignedInt
  val minimum = buf.readUnsignedInt
  

}