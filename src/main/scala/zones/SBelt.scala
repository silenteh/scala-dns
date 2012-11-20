package zones
import scala.collection.immutable.List
import java.io.File
import configs.ConfigService
import com.codahale.jerkson.Json._
   
object SBelt {
  
  lazy val rootServers = listOfServer
  
  private def listOfServer(): List[Zone] = {
      val servers = List.empty[Zone]
      val file = ConfigService.config.getString("rootservers")
      val lines = io.Source.fromFile(file).getLines.toList	  
	  
	  def loop(acc: List[Zone], lines: List[String]): List[Zone] = {
    	  if(lines.length == 0)
    	    acc
    	   else
    	    loop(parse[Zone](lines.head) :: acc, lines.tail)
	  }
	  loop(servers,lines)	  
	  
  }
  
}