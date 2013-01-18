package domainio

import org.slf4j.LoggerFactory
import configs.ConfigService
import java.io.File
import scala.collection.JavaConversions._
import java.util.LinkedHashMap
import java.util.ArrayList
import models.Domain
import models.Host
import java.text.SimpleDateFormat
import models.ExtendedDomain
import datastructures.DNSCache
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.annotation.JsonInclude
import enums.RecordType
import scala.annotation.tailrec
import models.User
import datastructures.UserCache

object JsonIO {

  val logger = LoggerFactory.getLogger("app")
  val applicationRoot = new File("").getAbsolutePath()
  val dataPathStr = ConfigService.config.getString("zoneFilesLocation")
  val userPathStr = ConfigService.config.getString("userFileLocation")
  
  val dataPath = new File(applicationRoot + dataPathStr)
  val userPath = new File(applicationRoot + userPathStr)
  if(!dataPath.exists) dataPath.mkdirs
  
  val Json = {
    val m = new ObjectMapper()
    //m.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.registerModule(DefaultScalaModule)
    m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
  
  def loadUsers(userFile: File = userPath) = 
    if(userFile.exists) 
      Json.readValue(userFile, classOf[Array[User]]).foreach(user => UserCache.addUser(user))
  
  def loadDomain(domainfile: File, domainNames: Array[String]) = {
    try {
      val domain = Json.readValue(domainfile, classOf[ExtendedDomain])
      /*if (DomainValidationService.validate(domain, domainNames)) */DNSCache.setDomain(domain)
      //else logger.warn("Misplaced entry: " + domainfile.getAbsolutePath)
      //if (validate(domain, domainNames) < 2) DNSCache.setDomain(domain)
    } catch {
      case ex: JsonParseException => logger.warn("Broken json file: " + domainfile.getAbsolutePath)
    }
  }
  
  def storeDomain(domain: ExtendedDomain, path: String = dataPathStr) = {
    val filename = 
      if(domain.fullName.startsWith("*")) "-wildcard" + domain.fullName.substring(1) + "json"
      else domain.fullName + "json"
    logger.debug(applicationRoot + path + "/" + filename)
    Json.writeValue(new File(applicationRoot + path + "/" + filename), domain)
  }
  
  def removeDomain(domainName: String, path: String = dataPathStr) = {
    val filename = 
      if(domainName.startsWith("*")) "-wildcard" + domainName.substring(1) + "json"
      else domainName + "json"
    val domainFile = new File(applicationRoot + path + "/" + domainName + "json");
    domainFile.delete
  }
  
  def updateUsers(userFile: File = userPath) = 
    Json.writeValue(userFile, UserCache.users.toArray)
  
}