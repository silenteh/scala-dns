package payload
import org.jboss.netty.buffer.ChannelBuffer
import records._

class RRData(buf: ChannelBuffer) {
  
  val name = Name.parse(buf)
  val rtype = buf.readUnsignedShort
  val rclass = buf.readUnsignedShort
  val ttl = buf.readUnsignedInt()
  val rdlength = buf.readUnsignedShort
  val rdata = deserializeRecord(buf, rtype,rclass,rdlength)
  
  
  private def deserializeRecord(buf: ChannelBuffer, recordtype: Int, recordclass: Int, size: Int) = {
    
    
    val data = recordtype match {
    	// A 
    	case 1 => new A(buf,recordclass,size)
    	  
    	// NS
    	case 2 => new NS(buf,recordclass,size)
    	
    	// MD
    	case 3 => null // NYI
    	
    	// MF
    	case 4 => null // NYI
    	  
    	// CNAME
    	case 5 => new CNAME(buf,recordclass,size)
    	  
    	// SOA
    	case 6 => new SOA(buf,recordclass,size)
    	  
    	// MB
    	case 7 => null
    	  
    	// MG
    	case 8 => null 
    	  
    	// MR  
    	case 9 => null
    	  
    	// NULL
    	case 10 => new NULL(buf,recordclass,size)
    	  
    	// WKS
    	case 11 => null
    	
    	// PTR
    	case 12 => new PTR(buf,recordclass,size)
    	  
    	//HINFO
    	case 13 => null
    	  
    	// MINFO
    	case 14 => null 
    	  
    	// MX
    	case 15 => new MX(buf,recordclass,size)
    	
    	// TXT
    	case 16 => new TXT(buf,recordclass,size)
    	
    	case 28 => new AAAA(buf,recordclass,size)
    	
    	// AXFR
    	case 252 => null
    	
    	// *
    	case 255 => null    	
    	  
    }
    
    data
    
    
  }
  
  
//  private def deserializeBasedOnClass(buf: ChannelBuffer, rrecordclass: Int, size: Int) = {
//    
//    rrecordclass match {
//    	case 1 =>
//    	  
//    	case 2 =>
//    	  
//    	case 3 =>
//    	  
//    	case 4 =>
//    	  
//    	case 255 =>
//    }
//    
//  }
  

  // NAME            a domain name to which this resource record pertains.
  //var name = ""
    
  // TYPE         two octets containing one of the RR type codes.  This
  //              field specifies the meaning of the data in the RDATA
  //              field.  
  //var rtype= ""
  
    
  // CLASS        two octets which specify the class of the data in the
  //              RDATA field.
  //var rclass = ""
    
    
  // TTL          a 32 bit unsigned integer that specifies the time
  //              interval (in seconds) that the resource record may be
  //              cached before it should be discarded.  Zero values are
  //              interpreted to mean that the RR can only be used for the
  //              transaction in progress, and should not be cached.
  //var ttl = 0
  
  
  // RDLENGTH     an unsigned 16 bit integer that specifies the length in
  //              octets of the RDATA field.
  //var rdlenght = 0
  
  // RDATA        a variable length string of octets that describes the
  //              resource.  The format of this information varies
  //              according to the TYPE and CLASS of the resource record.
  //              For example, the if the TYPE is A and the CLASS is IN,
  //              the RDATA field is a 4 octet ARPA Internet address.
  //var rdata = ""
}