package datastructures

import scala.collection.immutable.TreeMap
import models.ExtendedDomain
import scala.collection.immutable.Map
import scala.annotation.tailrec

object DNSAuthoritativeSection extends DNSDomainStorage[ExtendedDomain] {

  protected var domains = TreeMap[String,Map[String, ExtendedDomain]]("." -> Map())
  
  @tailrec
  protected def findDomainName(typ: Int, parts: List[String], storedMap: Map[String, ExtendedDomain], name: Seq[String]): Option[ExtendedDomain] = 
    if(name.isEmpty) None
    else storedMap.get(name.mkString(".")) match {
      case Some(domain) => {
        if(parts.size - 1 != name.size || domain.hasRootEntry(typ)) Some(domain)
        else findDomainName(typ, parts, storedMap, name.tail)
      }
      case _ => findDomainName(typ, parts, storedMap, name.tail)
    }
  
  protected def addDomainEntry(domain: ExtendedDomain) = (domain.name -> domain)

}