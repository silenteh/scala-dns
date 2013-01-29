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
package domainio

import org.slf4j.LoggerFactory
import configs.ConfigService
import java.io.File
import scala.collection.JavaConversions._
import java.text.SimpleDateFormat
import models.ExtendedDomain
import datastructures.DNSCache
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.annotation.JsonInclude
import models.User
import datastructures.UserCache
import datastructures.DNSAuthoritativeSection

object JsonIO {

  val logger = LoggerFactory.getLogger("app")
  val applicationRoot = new File("").getAbsolutePath()
  val dataPathStr = ConfigService.config.getString("zoneFilesLocation")
  val authDataPathStr = dataPathStr + "/authoritative"
  val cacheDataPathStr = dataPathStr + "/cache"
  val sbeltDataPathStr = dataPathStr + "/sbelt"
  
  val userPathStr = ConfigService.config.getString("userFileLocation")
  
  val dataPath = new File(applicationRoot + dataPathStr)
  val authDataPath = new File(applicationRoot + authDataPathStr)
  val cacheDataPath = new File(applicationRoot + cacheDataPathStr)
  val sbeltDataPath = new File(applicationRoot + sbeltDataPathStr)
  
  val userPath = new File(applicationRoot + userPathStr)
  
  if(!dataPath.exists) dataPath.mkdirs
  if(!authDataPath.exists) authDataPath.mkdirs
  if(!cacheDataPath.exists) cacheDataPath.mkdirs
  if(!sbeltDataPath.exists) sbeltDataPath.mkdirs
  
  val Json = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
    m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    m.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
    m
  }
  
  logger.info(dataPath.getAbsolutePath())
  
  def loadData = {
    loadDataOfType(authDataPath, classOf[ExtendedDomain]) { DNSAuthoritativeSection.setDomain(_) }
    loadDataOfType(cacheDataPath, classOf[ExtendedDomain]) { DNSCache.setDomain(_) }
    DNSAuthoritativeSection.logDomains
  }
  
  def loadDataOfType[T](domainfolder: File = dataPath, typ: Class[T])(fn: T => Unit) = {
    val files = domainfolder.listFiles.filter(_.getName.endsWith(".json"))
    files.foreach(loadItem(_, typ)(fn))
  }
  
  def loadItem[T](file: File, typ: Class[T])(fn: T => Any) = 
    try {
      val item = Json.readValue(file, typ)
      fn(item)
    } catch {
      case ex: JsonParseException => logger.warn("Broken json file: " + file.getAbsolutePath)
    }
  
  def loadUsers(userFile: File = userPath) = 
    if(userFile.exists) 
      Json.readValue(userFile, classOf[Array[User]]).foreach(user => UserCache.addUser(user))
  
  def storeData[T](data: T, name: String, path: String) = {
    logger.debug(applicationRoot + path + "/" + name + "json")
    Json.writeValue(new File(applicationRoot + path + "/" + name + "json"), data)
  }
  
  def storeAuthData(domain: ExtendedDomain) = storeData(domain, domain.getFilename, authDataPathStr)
  def storeCacheData(domain: ExtendedDomain) = storeData(domain, domain.getFilename, cacheDataPathStr)
  //def storeSBeltData(domain: ExtendedDomain) = storeData(domain, domain.getFilename, authDataPathStr)
  
  def removeData(name: String, path: String) = {
    val file = new File(applicationRoot + path + "/" + name + "json");
    file.delete
  }
  
  def removeAuthData(name: String) = 
    removeData(if(name.startsWith("*")) "-wildcard" + name.substring(1) else name, authDataPathStr)
  def removeCacheData(name: String) = 
    removeData(if(name.startsWith("*")) "-wildcard" + name.substring(1) else name, cacheDataPathStr)
  //def removeSBeltData(domain: ExtendedDomain) = removeData(domain.getFilename, authDataPathStr)
  
  def updateUsers(userFile: File = userPath) = Json.writeValue(userFile, UserCache.users.toArray)
  
}