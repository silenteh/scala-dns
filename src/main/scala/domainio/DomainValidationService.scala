package domainio

import models.ExtendedDomain
import scala.annotation.tailrec
import datastructures.DNSCache
import enums.RecordType
import models.Host
import org.slf4j.LoggerFactory

object DomainValidationService {

  val logger = LoggerFactory.getLogger("app")
  
  def validate(domain: ExtendedDomain) = {
    val unreachable = checkUnreachable(domain)
    val hasDuplicities = checkDuplicities(domain, unreachable)
    hasDuplicities
  }
  
  def checkUnreachable(domain: ExtendedDomain, validationStatus: (Boolean, List[String]) = (true, Nil)) = {
    val domainNames = DNSCache.getDomainNames
      
    /*def findName(domainNameParts: Array[String]): Boolean = {
      if(domainNameParts.isEmpty) true
      else if(domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName.substring(0, domain.fullName.length - 1))) false
      else findName(domainNameParts.tail)
    }*/
    //domain.hosts.forall {host => findName(host.name.split("""\.""").tail)}
    
    def findName(domainNameParts: Array[String]): String = 
      if(domainNameParts.isEmpty) domain.fullName
      else if(!domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName.substring(0, domain.fullName.length - 1)))
        findName(domainNameParts.tail)
      else domainNameParts.mkString(".") + "." + domain.fullName
    
    domain.hosts.foldLeft(validationStatus) {(status, host) => 
      val name = findName(host.name.split("""\.""").tail)
      val valid = name == domain.fullName
      val message = 
        if(valid) null
        else "Unreachable entry '%s' in %sjson zone file. Rename it and move it to %s zone."
          .format(host.name, domain.fullName, name)
      (status._1 && valid, if(valid) status._2 else message :: status._2)
    }
  }
  
  def checkDuplicities(domain: ExtendedDomain, validationStatus: (Boolean, List[String]) = (true, Nil)) = {
    val duplicities = domain.hosts.map {host =>
      RecordType.values.find(_.toString == host.typ) match {
        case None => null
        case Some(typ) => {
          val qname = 
            if(host.name == domain.fullName || host.name == "@") domain.fullName
            else host.name + "." + domain.fullName
            
          DNSCache.findDomain(typ.id, qname.split("""\.""").toList) match {
            case None => null
            case Some(existingDomain) => {
              if(existingDomain.fullName == domain.fullName) null
              else {
                val hostname = qname.substring(0, qname.lastIndexOf(existingDomain.fullName) - 1)
            	existingDomain.findHost(hostname, typ.id) match {
                  case None => null
                  case Some(host) => (qname, existingDomain)
                }
              }
            }
          }
        }
      }
    }.filterNot(_ == null)
    
    if(duplicities.isEmpty) validationStatus
    else (false, validationStatus._2 ++ duplicities.map { case (path, domain) =>
      "A record pointing to " + path + " already exists in the " + domain.fullName + " zone."
    })
  }
    
  /*def validate(domain: ExtendedDomain, domainNames: Array[String]) = {
    def findName(domainNameParts: Array[String]): String = 
      if(domainNameParts.isEmpty) domain.fullName
      else if(!domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName)) findName(domainNameParts.tail)
      else domainNameParts.mkString(".") + "." + domain.fullName
    
    val unreachable = domain.hosts.foldLeft(true) {(allValid, host) => 
      val name = findName(host.name.split("""\.""").tail)
      val valid = name == domain.fullName
      if(!valid) logger.warn(
        "Unreachable entry '%s' in %sjson zone file. Rename it and move it to %sjson zone file."
          .format(host.name, domain.fullName, name))
      valid && allValid
    }
    
    if(unreachable) 1 else 0
  }*/
  
  def validateName(name: String, allowAbsolute: Boolean = false) = {
    
  }
  
  def reorganize(srcdomain: ExtendedDomain) = {
    logger.debug("Reorganizing")
    val name = srcdomain.nameParts.toList
    
    @tailrec
    def ancestorDomains(parts: List[String], domains: List[(String, ExtendedDomain)] = List()): List[(String, ExtendedDomain)] = {
      if(parts.isEmpty) domains
      else DNSCache.findDomain(RecordType.ALL.id, parts) match {
        case Some(domain) => {
          val index = name.mkString(".").lastIndexOf(parts.mkString(".")) - 1
          ancestorDomains(parts.tail, (name.mkString(".").substring(0, index), domain) :: domains)
        }
        case None => 
          ancestorDomains(parts.tail, domains)
      }
    }
    
    val (hosts, domains) = ancestorDomains(name.tail)
      .foldRight((List[(String, Host)](), List[ExtendedDomain]())) { case((residue, domain), (allhosts, domains)) => 
        val hosts = domain.hosts.filter{host => 
          (host.name == residue && srcdomain.hasRootEntry(RecordType.values.find(_.toString == host.typ).get.id)) || 
          (host.name != residue && host.name.endsWith(residue))
        }.map(host => (host.name + "." + domain.fullName, host))
        
        val newdomains = 
          if(hosts.isEmpty) domains
          else {
            val newdomain = hosts.foldRight(domain) { case((name, host), domain) => domain.removeHost(host) }
            DNSCache.setDomain(newdomain)
            DomainIO.storeDomain(newdomain)
            newdomain :: domains
          }
        (allhosts ++ hosts, newdomains)
      }
    
    hosts.foldRight(srcdomain) { case((fullname, host), domain) =>
      if(fullname == domain.fullName && domain.hasRootEntry(RecordType.values.find(_.toString == host.typ).get.id)) domain
      else {
        val newName = fullname.substring(0, fullname.lastIndexOf(domain.fullName) - 1);
        val newhost = host.setName(newName)
        domain.hosts.find(h => h.name == newhost.name && h.typ == newhost.typ) match {
          case None => domain.addHost(newhost)
          case Some(host) => domain.removeHost(host).addHost(newhost)
        }
      } 
    } :: domains
  }
  
  def reorganizeAll = 
    DNSCache.getDomains.foreach { case(extension, domains) =>
      domains.foreach { case(name, (timestamp, domain)) =>
        val newdomain = reorganize(domain)
        if(newdomain.head.hosts.length != domain.hosts.length) DomainIO.storeDomain(domain)
      }
    } 
}