package datastructures
import scala.collection.immutable.TreeMap
import models.ExtendedDomain
import scala.collection.mutable.Map
import scala.Tuple2

object DNSCache {
  

  val domains = TreeMap.empty[String,Map[String,Tuple2[Long,ExtendedDomain]]]
  
  def getDomain(extension: String, name: String):ExtendedDomain = {
    val domainMap = domains.get(extension)
    val ed = domainMap.get(name)
    val diff = System.currentTimeMillis() - ed._1 - ed._2.TTL
    val domain = if(diff > 0) {
      ed._2
    } else {
      domainMap.get -= (name)
      null
    }
    domain
  }
  
  def setDomain(extension: String, name: String, domain: ExtendedDomain) = {
    val storedMap = domains.get(extension).get
    val domainMap = if(storedMap == null) {
    	Map[String,ExtendedDomain](name -> domain)
    } else {
      storedMap += (name -> Tuple2[Long,ExtendedDomain](System.currentTimeMillis(),domain))
    }    
   domainMap
  }
}