package payload
import org.jboss.netty.buffer.ChannelBuffer

class Question(buf: ChannelBuffer) {
  
  
  val qname = Name.parse(buf) 
  val qtype = buf.readUnsignedShort
  val qclass = buf.readUnsignedShort
  
  
  
  //  QNAME         a domain name represented as a sequence of labels, where
  //                each label consists of a length octet followed by that
  //                number of octets.  The domain name terminates with the
  //                zero length octet for the null label of the root.  Note
  //                that this field may be an odd number of octets; no
  //                padding is used.
  //var qname = ""
    
  // QTYPE        a two octet code which specifies the type of the query.
  //              The values for this field include all codes valid for a
  //              TYPE field, together with some more general codes which
  //              can match more than one type of RR.  
  //var qtype = ""
    
  // QCLASS       a two octet code that specifies the class of the query.
  //              For example, the QCLASS field is IN for the Internet.
  //var qclass = ""

}