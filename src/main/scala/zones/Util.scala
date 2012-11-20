package zones
import com.codahale.jerkson.Json._


object Util {
  
  def fromJsonFile(fileName: String) = {    
    val jsonInput = io.Source.fromFile(fileName).mkString
    val parsedObj = parse[Zone](jsonInput)
    parsedObj    
  }
  
  
  def toJsonFile(fileName: String, zone: Zone) = {    
    val zoneJson = generate(zone)
    val output = new java.io.BufferedWriter(new java.io.FileWriter(new java.io.File(fileName)))
    output.write(zoneJson)
    output.flush
    output.close    
  }
  

}