package payload
import org.jboss.netty.buffer.ChannelBuffer
import enums.RecordType

// This class reassemble the network frames
class Message(buf: ChannelBuffer) {
  
	
	
	  val header = new Header(buf)
	  //println("Number of queries: " + header.qdcount)
	  val query = deserializeQuestions(buf, header.qdcount)
	  val answers = deserializeRRData(buf, header.ancount)
	  val authority = deserializeRRData(buf, header.nscount)
	  val additional = deserializeRRData(buf, header.arcount)
	  
	  
	  
//	  println(header.id)
//	  println(header.opcode)	  	  
//	  println(new String(query(0).qname(0), "UTF-8") + "." + new String(query(0).qname(1), "UTF-8"))
//	  
//	  println(query(0).qtype)
//	  println(query(0).qclass)
	  
	  
	
	 
  
	
	def deserializeQuestions(buf: ChannelBuffer, n: Int) = {	  
	    if(n >= 1) {
	    	val dataArray = new Array[Question](n)
	    	for(i <- 0 until n) {
	    		val query = new Question(buf)
	    		dataArray(i) = query
	    	}
	    	dataArray
	    } else {
	      val dataArray = Array.empty[Question]
	      dataArray
	    }
	  	  
	}
	
	
	def deserializeRRData(buf: ChannelBuffer, n: Int) = {
	  if(n < 1) {
	    val dataArray = new Array[RRData](n)
		  for(i <- 0 until n) {
		    val data = new RRData(buf)
		    dataArray(i) = data
		  }
		  dataArray
	  } else {
	      val dataArray = Array.empty[RRData]
	      dataArray
	  }		  	 
	}
	
  
	
	override def toString() = {
	  
	  var output = header.opcode match {
	  	case 0 => "Standard query"
	  	case 1 => "Inverse query"
	  	case 2 => "Status"
	  	case 4 => "Notify"
	  	case 5 => "Update"	  		  	
	  }
	  
	  
	  
	  
	  
	  
	  output = output + " - " + domainName
	  output = output + " - type: " + RecordType.apply(query(0).qtype).toString	  	  
	  output
	}
	
	private def domainName() = {
	    def loop(acc: String, arr: List[Array[Byte]]): String = {
	      if(arr.length == 0) {
	        acc
	      } else {
	        val string = if(acc == "") {
	          acc + new String(arr.head, "UTF-8")
	        } else {
	          acc + "." +new String(arr.head, "UTF-8")
	        }	        
	        loop(string, arr.tail)
	      }
	    } 
	    
	    loop("",query(0).qname)	    
	  }
  
}