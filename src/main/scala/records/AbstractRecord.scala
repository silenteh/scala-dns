package records
import org.jboss.netty.buffer.ChannelBuffer

abstract class AbstractRecord(buf: ChannelBuffer, recordclass: Int, size: Int) {
  
  lazy val MAX_STRING_LENGTH = 255
  
}