package datastructures

import scala.collection.immutable.TreeMap
import models.ExtendedDomain
import org.slf4j.LoggerFactory

trait DNSDomainStorage[T] {

  val logger = LoggerFactory.getLogger("app")
  
  protected var domains: TreeMap[String, Map[String, T]]
  
  protected def findDomainName(typ: Int, parts: List[String], storedMap: Map[String, T], name: Seq[String]): Option[ExtendedDomain]
  
  protected def addDomainEntry(domain: ExtendedDomain): (String, T)
  
  def findDomain(typ: Int, extension: String, name: String): Option[ExtendedDomain] = 
    findDomain(typ, (name.split("""\.""") :+ extension).toList)          
  
  def findDomain(typ: Int, parts: String*): Option[ExtendedDomain] = 
    findDomain(typ, parts.toList)
  
  def findDomain(typ: Int, parts: List[String]): Option[ExtendedDomain] = {
    val extension = parts.reverse.head
    val name = parts.take(parts.size - 1)
    domains.get(extension) match {
      case None => None
      case Some(storedMap) => 
        findDomainName(typ, parts, storedMap, name)
    }
  }
  
  def getDomain(typ: Int, extension: String, name: String): ExtendedDomain = 
    getDomain(typ, (name.split("""\.""") :+ extension).toList)
  
  def getDomain(typ: Int, parts: String*): ExtendedDomain = 
    getDomain(typ, parts.toList)
  
  def getDomain(typ: Int, parts: List[String]): ExtendedDomain = 
    findDomain(typ, parts) match {
      case Some(domain) => domain
      case None => throw new DomainNotFoundException
    }
  
  def setDomain(domain: ExtendedDomain) = {
    val storedMap = domains.get(domain.extension).getOrElse(Map())      
    val updatedMap = storedMap + addDomainEntry(domain)            
    domains = domains + (domain.extension -> updatedMap)
    updatedMap
  }
  
  def removeDomain(extension: String, name: String): Unit = 
    domains.get(extension) match {
      case Some(storedMap) => 
        if(storedMap.contains(name)) {
          val updatedMap = storedMap - name
          domains = 
            if(updatedMap.size == 0) domains - extension
            else domains + (extension -> updatedMap)
        }
      case _ => Unit
    }
  
  def removeDomain(parts: List[String]): Unit = {
    if(!parts.isEmpty) {
      val trimmedParts = if(parts.reverse.head == "") parts.take(parts.size - 1) else parts
      val extension = trimmedParts.reverse.head
      val name = trimmedParts.take(parts.size - 1).mkString(".")
      removeDomain(extension, name)
    }
  }
  
  def logDomains = logger.debug(domains.toString)
  
  def getDomains = domains
  
  def getDomainNames = domains.map { case(extension, domains) =>
    domains.map { case(name, domain) =>
      name + "." + extension
    }
  }.flatten.toArray
}

class DomainNotFoundException extends Exception