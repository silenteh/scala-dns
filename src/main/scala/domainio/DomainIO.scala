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
    val domains = domainfolder.listFiles
    domains.filter(_.getName.endsWith(".json")).foreach(loadDomain(_))
    DNSCache.logDomains
  }
  
  def loadDomain(domainfile: File) = {
    try {
      DNSCache.setDomain(Json.readValue(domainfile, classOf[ExtendedDomain]))
    } catch {
      case ex: JsonParseException => logger.warn("Broken json file: " + domainfile.getAbsolutePath)
    }
  }
  
  def storeDomain(domain: ExtendedDomain, path: String = dataPathStr) = {
    Json.writeValue(new File(path + domain.fullName + "json"), domain)
  }
}