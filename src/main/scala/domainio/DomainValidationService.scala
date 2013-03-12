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

import models.ExtendedDomain
import scala.annotation.tailrec
import datastructures.DNSCache
import enums.RecordType
import models._
import org.slf4j.LoggerFactory
import utils.HostnameUtils
import datastructures.DNSAuthoritativeSection

object DomainValidationService {

  val logger = LoggerFactory.getLogger("app")

  def validate(domain: ExtendedDomain, filename: String = null) = {
    if (domain == null) (2, validationMessages("unknown") :: Nil)
    else {
      val domainName = checkDomainName(domain.fullName, true, false)
      //val nameValidHostname = checkHostName(domain.fullName)
      val unique =
        //if (filename == null) isUnique(domain.fullName, DNSCache.getDomainNames.toList)
        //else isUnique(domain.fullName, DNSCache.getDomainNames.toList, filename :: Nil)
        if (filename == null) isUnique(domain.fullName, DNSAuthoritativeSection.getDomainNames.toList)
        else isUnique(domain.fullName, DNSAuthoritativeSection.getDomainNames.toList, filename :: Nil)
      val body = domain.hosts.foldRight((0, List[String]())) {
        case (host, (valid, messages)) =>
          val check = host match {
            case h: AddressHost => checkAddressHost(h, domain)
            case h: IPv6AddressHost => checkIPv6AddressHost(h, domain)
            case h: CnameHost => checkCnameHost(h, domain)
            case h: MXHost => checkMXHost(h, domain)
            case h: NSHost => checkNSHost(h, domain)
            case h: PointerHost => checkPointerHost(h, domain)
            case h: SoaHost => checkSoaHost(h, domain)
            case h: TxtHost => checkTxtHost(h, domain)
            case _ => checkGenericHost(host, domain)
          }
          (scala.math.max(valid, check._1), messages ++ check._2)
      }
      val unreachable = checkUnreachable(domain)
      val duplicate = checkRecordDuplicities(domain)
      val infinite = checkInfiniteLoops(domain)
      val incomplete = 
        if(domain.settings == null || domain.settings.isEmpty || domain.nameservers == null || 
            (domain.nameservers.length < 2 && domain.nameservers.head.hostnames.length < 2)) 
          (2, validationMessages("incomplete") :: Nil)
        else
          (0, Nil)
          
          List(1, 2, 1, 0, 1).max
          
      ((domainName._1 :: unique._1 :: body._1 :: unreachable._1 :: duplicate._1 :: incomplete._1 :: infinite._1 :: Nil).max, 
       (domainName._2 :: unique._2 :: (body._2 ++ unreachable._2 ++ duplicate._2 ++ incomplete._2 ++ infinite._2))
         .filterNot(_ == null).distinct)
    }
  }
  
  
  def checkAddressHost(host: AddressHost, domain: ExtendedDomain) = {
    val names = domain.address.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.ips == null || host.ips.isEmpty) (2, validationMessages("empty").format("IP address") :: Nil)
    else host.ips.foldRight(name._1, List(name._2)) {case (ip, (valid, messages)) =>
      val ipParts = ip.ip.split("""\.""")
      if (ipParts.length == 4 && ipParts.forall(_.matches("""^[0-9]+$"""))) (valid, messages)
      else (2, validationMessages("ipv4_not_valid").format(ip.ip) :: messages)
    }
  }
  
  def checkIPv6AddressHost(host: IPv6AddressHost, domain: ExtendedDomain) = {
    val names = domain.ipv6address.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.ips == null || host.ips.isEmpty) (2, validationMessages("empty").format("IP address") :: Nil)
    else host.ips.foldRight(name._1, List(name._2)) {case (ip, (valid, messages)) =>
      val ipParts = ip.ip.split("""\:""")
      if ((ipParts.length == 6 || (ip.ip.contains("::") && ip.ip.indexOf("::") == ip.ip.lastIndexOf("::"))) &&
        ipParts.forall(p => p == "" || p.matches("""^[0-9a-fA-F]{1,4}$"""))) (valid, messages)
      else (2, validationMessages("ipv6_not_valid").format(ip.ip) :: messages)
    }
  }
  
  def checkCnameHost(host: CnameHost, domain: ExtendedDomain) = {
    val names = domain.cname.map(_.name).toList
    val name = checkHostName(host, names, true)
    val cname = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    val uniqueHostname = 
      if(domain.cname.filterNot(_.equals(host)).exists(c =>
        c.name == host.name && 
        HostnameUtils.absoluteHostName(c.hostname, domain.fullName) == HostnameUtils.absoluteHostName(host.hostname, domain.fullName)
      )) (2, validationMessages("duplicate").format(host.name))
      else (0, null)
    ((name._1 :: cname._1 :: unique._1 :: uniqueHostname._1 :: Nil).max, name._2 :: cname._2 :: unique._2 :: uniqueHostname._2 :: Nil)
  }
  
  def checkMXHost(host: MXHost, domain: ExtendedDomain) = {
    val names = domain.mailx.map(_.name).toList
    val name = checkHostName(host, names)
    val mx = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    ((name._1 :: mx._1 :: unique._1 :: Nil).max, name._2 :: mx._2 :: unique._2 :: Nil)
  }
  
  /*def checkNSHost2(host: NSHost, domain: ExtendedDomain) = {
    val names = domain.nameservers.map(_.name).toList
    val name = checkHostName(host, names)
    val ns = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    (name._1 && ns._1 && unique._1, name._2 :: ns._2 :: unique._2 :: Nil)
  }*/
  
  def checkNSHost(host: NSHost, domain: ExtendedDomain) = {
    val names = domain.nameservers.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.hostnames == null || host.hostnames.isEmpty) (2, validationMessages("empty").format("IP address") :: Nil)
    else host.hostnames.foldRight(name._1, List(name._2)) {case (hostname, (valid, messages)) =>
      val ns = checkDomainName(hostname.hostname)
      val unique = isUnique(hostname.hostname, names)
      ((valid :: ns._1 :: unique._1 :: Nil).max, ns._2 :: unique._2 :: messages)
    }
  }
  
  def checkPointerHost(host: PointerHost, domain: ExtendedDomain) = {
    val names = domain.pointer.map(_.name).toList
    val name = checkHostName(host, names)
    val ptr = checkDomainName(host.ptrdname)
    val unique = isUnique(host.ptrdname, names)
    ((name._1 :: ptr._1 :: unique._1 :: Nil).max, name._2 :: ptr._2 :: unique._2 :: Nil)
  }
  
  def checkSoaHost(host: SoaHost, domain: ExtendedDomain) = {
    val names = domain.settings.map(_.name).toList
    val name = checkHostName(host, names)
    val mname = checkDomainName(host.mname)
    val rname = checkDomainName(host.rname)
    val ttl = checkTimeValue(host.ttl)
    val refresh = checkTimeValue(host.refresh)
    val retry = checkTimeValue(host.retry)
    val minimum = checkTimeValue(host.minimum)
    val expire = checkTimeValue(host.expire)
    ((name._1 :: mname._1 :: rname._1 :: ttl._1 :: refresh._1 :: retry._1 :: minimum._1 :: expire._1 :: Nil).max,
     name._2 :: mname._2 :: rname._2 :: ttl._2 :: refresh._2 :: retry._2 :: minimum._2 :: expire._2 :: Nil)
  }
  
  def checkTxtHost(host: TxtHost, domain: ExtendedDomain) = {
    val names = domain.text.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.strings != null && !host.strings.isEmpty) (name._1, name._2 :: Nil)
    else (2, validationMessages("empty").format("Text") :: Nil)
  }
  
  def checkGenericHost(host: Host, domain: ExtendedDomain) = {
    val names = domain.text.map(_.name).toList
    val name = checkHostName(host, names)
    (name._1, name._2 :: Nil)
  }
  
  def checkDomainName(name: String, absolute: Boolean = true, relative: Boolean = true) = {
    if (name == null)
      (2, validationMessages("required").format("domain name"))
    else if (name.length > 255)
      (2, validationMessages("name_long").format(name))
    else if (name.split("""\.""").exists(_.length > 63))
      (2, validationMessages("name_part_long").format(name))
    else if (!relative && absolute && name != "@" && name.lastIndexOf(".") != name.length - 1)
      (2, validationMessages("name_not_absolute").format(name))
    else if (relative && !absolute && name != "@" && name.lastIndexOf(".") == name.length - 1)
      (2, validationMessages("name_not_relative").format(name))
    else
      (0, null)
  }

  def checkHostName(host: Host, names: List[String], ignoreUnique: Boolean = false, absolute: Boolean = true, relative: Boolean = true) = {
    val domainCheck = checkDomainName(host.name, absolute, relative)
    if(domainCheck._1 == 2) domainCheck
    else {
      val uniqueCheck = 
        if(ignoreUnique) (0, null)
        else isUnique(host.name, names)
      if(uniqueCheck._1 == 2) uniqueCheck
      else if (host.name == "@" || host.name.matches("""([a-zA-Z0-9\*]{1}([a-zA-Z0-9\-\*]*[a-zA-Z0-9\*]{1})*\.{0,1})*""")) (0, null)
      else (2, validationMessages("name_not_hostname").format(host.name))
    }
  }

  def isUnique(item: String, items: List[String], exclude: List[String] = Nil) =
    if (exclude.contains(item) || items.filter(_ == item).length <= 1) (0, null)
    else (2, validationMessages("duplicate").format(item))

  def checkTimeValue(time: String) =
    if (time != null && time.matches("""^([0-9]+[hdmswHDMSW]{0,1})+$""")) (0, null)
    else (2, validationMessages("ttl_not_valid").format(time))

  def checkUnreachable(domain: ExtendedDomain) = {
    //val domainNames = DNSCache.getDomainNames
    val domainNames = DNSAuthoritativeSection.getDomainNames

    def findName(domainNameParts: Array[String]): String =
      if (domainNameParts.isEmpty) domain.fullName
      else if (!domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName.substring(0, domain.fullName.length - 1)))
        findName(domainNameParts.tail)
      else domainNameParts.mkString(".") + "." + domain.fullName

    domain.hosts.foldLeft((0, List[String]())) { (status, host) =>
      val name = findName(host.name.split("""\.""").tail)
      val valid = if(name == domain.fullName) 0 else 2
      val message =
        if (valid == 0) null
        else validationMessages("unreachable").format(host.name, domain.fullName, name)
      (scala.math.max(status._1, valid), if (valid == 0) status._2 else message :: status._2)
    }
  }

  def checkRecordDuplicities(domain: ExtendedDomain) = {
    val duplicities = domain.hosts.map { host =>
      RecordType.values.find(_.toString == host.typ) match {
        case None => null
        case Some(hostTyp) => {
          val qname =
            if (host.name == domain.fullName || host.name == "@") domain.fullName
            else host.name + "." + domain.fullName

          val typ = if(hostTyp == RecordType.CNAME) RecordType.ALL else hostTyp
            
          //DNSCache.findDomain(typ.id, qname.split("""\.""").toList) match {
          DNSAuthoritativeSection.findDomain(typ.id, qname.split("""\.""").toList) match {
            case None => null
            case Some(existingDomain) => {
              if (existingDomain.fullName == domain.fullName) null
              else {
                val hostname = 
                  if(qname == existingDomain.fullName) "@"
                  else qname.substring(0, qname.lastIndexOf(existingDomain.fullName) - 1)
                existingDomain.findHost(hostname, typ.id) match {
                  case None => null
                  case Some(host) => (qname, typ.toString, existingDomain)
                }
              }
            }
          }
        }
      }
    }.filterNot(_ == null)

    if (duplicities.isEmpty) (0, Nil)
    else (2, duplicities.map {
      case (path, typ, domain) =>
        validationMessages("duplicate_record").format(typ, path, domain.fullName)
    })
  }

  def checkInfiniteLoops(domain: ExtendedDomain) = {
    val ahn = HostnameUtils.absoluteHostName _
    val dfn = domain.fullName
    
    @tailrec
    def resolveCname(name: String, checkedCnames: List[String] = List()): (Int, String) = 
      domain.cname.find(c => ahn(c.name, dfn) == name) match {
        case None => (0, null)
        case Some(cname) => 
          if(checkedCnames.contains(ahn(cname.hostname, dfn))) 
            (1, validationMessages("infinite").format((ahn(cname.hostname, dfn) :: name :: checkedCnames).reverse.mkString(" -> ")))
          else resolveCname(ahn(cname.hostname, dfn), name :: checkedCnames)
      }
    
    if(domain.cname == null) (0, Nil)
    else domain.cname.foldRight((0, List[String]())) { case(cname, (valid, messages)) =>
      val (curValid, curMessage) = resolveCname(ahn(cname.hostname, dfn), ahn(cname.name, dfn) :: Nil)
      (scala.math.max(curValid, valid), curMessage :: messages)
    }
    
  }
  
  def reorganize(srcdomain: ExtendedDomain) = {
    val name = srcdomain.nameParts.toList

    @tailrec
    def ancestorDomains(parts: List[String], domains: List[(String, ExtendedDomain)] = List()): List[(String, ExtendedDomain)] = {
      if (parts.isEmpty) domains
      //else DNSCache.findDomain(RecordType.ALL.id, parts) match {
      else DNSAuthoritativeSection.findDomain(RecordType.ALL.id, parts) match {
        case Some(domain) => {
          val index = name.mkString(".").lastIndexOf(parts.mkString(".")) - 1
          ancestorDomains(parts.tail, (name.mkString(".").substring(0, index), domain) :: domains)
        }
        case None =>
          ancestorDomains(parts.tail, domains)
      }
    }

    val (hosts, domains) = ancestorDomains(name.tail)
      .foldRight((List[(String, Host)](), List[ExtendedDomain]())) {
        case ((residue, domain), (allhosts, domains)) =>
          val hosts = domain.hosts.filter { host =>
            (host.name == residue && srcdomain.hasRootEntry(RecordType.values.find(_.toString == host.typ).get.id)) ||
              (host.name != residue && host.name.endsWith(residue))
          }.map(host => (host.name + "." + domain.fullName, host))

          val newdomains =
            if (hosts.isEmpty) domains
            else {
              val newdomain = hosts.foldRight(domain) { case ((name, host), domain) => domain.removeHost(host) }
              //DNSCache.setDomain(newdomain)
              DNSAuthoritativeSection.setDomain(newdomain)
              JsonIO.storeAuthData(newdomain)
              newdomain :: domains
            }
          (allhosts ++ hosts, newdomains)
      }

    hosts.foldRight(srcdomain) {
      case ((fullname, host), domain) =>
        if (fullname == domain.fullName && domain.hasRootEntry(RecordType.values.find(_.toString == host.typ).get.id)) domain
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
    //DNSCache.getDomains.foreach {
    DNSAuthoritativeSection.getDomains.foreach {
      case (extendion, domains) =>
        domains.foreach {
//          case (name, (timestamp, domain)) =>
          case (name, domain) =>
            val newdomain = reorganize(domain)
            if (newdomain.head.hosts.length != domain.hosts.length) JsonIO.storeAuthData(domain)
        }
    }
  
  val validationMessages = Map(
    "name_long" -> "Domain name %s is too long. The maximum allowed length is 255 characters.",
    "name_part_long" -> "One or more labels of %s domain name is too long. The maximum length allowed for label is 63 characters",
    "name_not_absolute" -> "Domain name %s is not absolute",
    "name_not_relative" -> "Domain name %s is not relative",
    "name_not_hostname" -> "Domain name %s is not a valid hostname",
    "ipv4_not_valid" -> "%s is not a valid IPv4 address",
    "ipv6_not_valid" -> "%s is not a valid IPv6 address",
    "number_not_valid" -> "%s is not a valid number",
    "ttl_not_valid" -> "%s is not a valid time value",
    "duplicate" -> "%s is not unique",
    "empty" -> "%s is required",
    "unreachable" -> "'%s' in %sjson zone file is unreachable. Rename it and move it to %s zone.",
    "duplicate_record" -> "%s record pointing to %s already exists in the %s zone.",
    "unknown" -> "Unknown data format",
    "incomplete" -> "The domain is missing required records",
    "infinite" -> "Infinite loop when resolving a CNAME: %s")
}