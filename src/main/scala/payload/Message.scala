/*******************************************************************************
 * Copyright 2012 silenteh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
