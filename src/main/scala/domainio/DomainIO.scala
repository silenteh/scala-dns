package domainio

import org.slf4j.LoggerFactory
import configs.ConfigService
import java.io.File
import scala.collection.JavaConversions._
import java.util.LinkedHashMap
import java.util.ArrayList
import models.Domain
import models.Host
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.DeserializationConfig
import java.text.SimpleDateFormat
import models.ExtendedDomain
import datastructures.DNSCache
import org.codehaus.jackson.map.SerializationConfig
import org.codehaus.jackson.map.JsonMappingException
import org.codehaus.jackson.JsonParseException

object DomainIO {

  val logger = LoggerFactory.getLogger("app")
  val applicationRoot = new File("").getAbsolutePath()
  val dataPathStr = ConfigService.config.getString("zoneFilesLocation")
  
  val dataPath = new File(applicationRoot + dataPathStr)
  if(!dataPath.exists) dataPath.mkdirs
  
  val Json = {
    val m = new ObjectMapper()
    m.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
    m
  }
  
  logger.info(dataPath.getAbsolutePath())
  
  def loadDomains(domainfolder: File = dataPath) = {
    val domainFiles = domainfolder.listFiles.filter(_.getName.endsWith(".json"))
    val domainNames = domainFiles.map(domainFile => domainFile.getName.take(domainFile.getName.indexOfSlice("json")))
    domainFiles.foreach(loadDomain(_, domainNames))
    DNSCache.logDomains
  }
  
  def loadDomain(domainfile: File, domainNames: Array[String]) = {
    try {
      val domain = Json.readValue(domainfile, classOf[ExtendedDomain])
      if (validate(domain, domainNames)) DNSCache.setDomain(domain)
      else logger.warn("Misplaced entry: " + domainfile.getAbsolutePath)
    } catch {
      case ex: JsonParseException => logger.warn("Broken json file: " + domainfile.getAbsolutePath)
    }
  }
  
  def storeDomain(domain: ExtendedDomain, path: String = dataPathStr) = {
    Json.writeValue(new File(path + domain.fullName + "json"), domain)
  }
  
  def validate(domain: ExtendedDomain, domainNames: Array[String]) = {
    def findName(domainNameParts: Array[String]): Boolean = 
      if(domainNameParts.isEmpty) true
      else if(domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName)) false
      else findName(domainNameParts.tail)
    
    domain.hosts.forall(host => findName(host.name.split("""\.""").tail))
  }
}