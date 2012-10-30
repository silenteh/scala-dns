package payload
import org.jboss.netty.buffer.ChannelBuffer

// This class reassemble the network frames
class Message(buf: ChannelBuffer) {
  
	
	
	  val header = new Header(buf)
	  val query = deserializeQuestions(buf, header.qdcount)
	  val answers = deserializeRRData(buf, header.ancount)
	  val authority = deserializeRRData(buf, header.nscount)
	  val additional = deserializeRRData(buf, header.arcount)
	  
	  
	  
	  println(header.id)
	  println(header.opcode)	  	  
	  
	  
	
  
	
	def deserializeQuestions(buf: ChannelBuffer, n: Int) = {	  
	    if(n < 1) {
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
	
  
  
}