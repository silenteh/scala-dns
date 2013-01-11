package domainio

import models.ExtendedDomain
import scala.annotation.tailrec
import datastructures.DNSCache
import enums.RecordType
import models.Host

object DomainValidationService {

  def validate(domain: ExtendedDomain, domainNames: Array[String]) = {
    def findName(domainNameParts: Array[String]): Boolean = 
      if(domainNameParts.isEmpty) true
      else if(domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName)) false
      else findName(domainNameParts.tail)
      
    domain.hosts.forall(host => findName(host.name.split("""\.""").tail))
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
    val name = srcdomain.nameParts.toList
    
    @tailrec
    def ancestorDomains(parts: List[String], domains: List[(String, ExtendedDomain)] = List()): List[(String, ExtendedDomain)] = 
      if(parts.isEmpty) domains
      else DNSCache.findDomain(RecordType.ALL.id, parts) match {
        case Some(domain) => ancestorDomains(parts.tail, (name.take(name.indexOfSlice(parts)).mkString("."), domain) :: domains)
        case None => ancestorDomains(parts.tail, domains)
      }
    
    val (hosts, domains) = ancestorDomains(name.tail)
      .foldRight((List[Host](), List[ExtendedDomain]())) { case((residue, domain), (allhosts, domains)) => 
        val hosts = domain.hosts.filter{host => 
          (host.name == residue && srcdomain.hasRootEntry(RecordType.values.find(_.toString == host.typ).get.id)) || 
          (host.name != residue && host.name.endsWith(residue))
        }
        val newdomains = 
          if(hosts.isEmpty) domains
          else{
            val newdomain = hosts.foldRight(domain) { case(host, domain) => domain.removeHost(host) }
            DNSCache.setDomain(newdomain)
            DomainIO.storeDomain(newdomain)
            domain :: domains
          }
        (allhosts ++ hosts, domains)
      }
    
    hosts.foldRight(srcdomain) { case(host, domain) =>
      if(host.name.startsWith(domain.nameParts.head)) domain
      else {
        val newhost = host.setName(host.name.substring(0, host.name.indexOf(domain.nameParts.head) - 1))
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