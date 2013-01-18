package domainio

import models.ExtendedDomain
import scala.annotation.tailrec
import datastructures.DNSCache
import enums.RecordType
import models._
import org.slf4j.LoggerFactory

object DomainValidationService {

  val logger = LoggerFactory.getLogger("app")

  def validate(domain: ExtendedDomain, filename: String = null) = {
    if (domain == null) (false, validationMessages("unknown") :: Nil)
    else {
      val domainName = checkDomainName(domain.fullName, true, false)
      //val nameValidHostname = checkHostName(domain.fullName)
      val unique =
        if (filename == null) isUnique(domain.fullName, DNSCache.getDomainNames.toList)
        else isUnique(domain.fullName, DNSCache.getDomainNames.toList, filename :: Nil)
      val body = domain.hosts.foldRight((true, List[String]())) {
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
          (valid && check._1, messages ++ check._2)
      }
      val unreachable = checkUnreachable(domain)
      val duplicate = checkRecordDuplicities(domain)
      val incomplete = 
        if(domain.settings == null || domain.settings.isEmpty || domain.nameservers == null || domain.nameservers.length < 2) 
          (false, validationMessages("incomplete") :: Nil)
        else
          (true, Nil)
      (domainName._1 && unique._1 && body._1 && unreachable._1 && duplicate._1 && incomplete._1, 
       (domainName._2 :: unique._2 :: (body._2 ++ unreachable._2 ++ duplicate._2 ++ incomplete._2)).filterNot(_ == null).distinct)
    }
  }
  
  
  def checkAddressHost(host: AddressHost, domain: ExtendedDomain) = {
    val names = domain.address.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.ips == null || host.ips.isEmpty) (false, validationMessages("empty").format("IP address") :: Nil)
    else host.ips.foldRight(name._1, List(name._2)) {case (ip, (valid, messages)) =>
      val ipParts = ip.ip.split("""\.""")
      if (ipParts.length == 4 && ipParts.forall(_.matches("""^[0-9]+$"""))) (valid, messages)
      else (false, validationMessages("ipv4_not_valid").format(ip.ip) :: messages)
    }
  }
  
  def checkIPv6AddressHost(host: IPv6AddressHost, domain: ExtendedDomain) = {
    val names = domain.ipv6address.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.ips == null || host.ips.isEmpty) (false, validationMessages("empty").format("IP address") :: Nil)
    else host.ips.foldRight(name._1, List(name._2)) {case (ip, (valid, messages)) =>
      val ipParts = ip.ip.split("""\:""")
      if ((ipParts.length == 6 || (ip.ip.contains("::") && ip.ip.indexOf("::") == ip.ip.lastIndexOf("::"))) &&
        ipParts.forall(p => p == "" || p.matches("""^[0-9a-fA-F]{1,4}$"""))) (valid, messages)
      else (false, validationMessages("ipv6_not_valid").format(ip.ip) :: messages)
    }
  }
  
  def checkCnameHost(host: CnameHost, domain: ExtendedDomain) = {
    val names = domain.cname.map(_.name).toList
    val name = checkHostName(host, names)
    val cname = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    (name._1 && cname._1 && unique._1, name._2 :: cname._2 :: unique._2 :: Nil)
  }
  
  def checkMXHost(host: MXHost, domain: ExtendedDomain) = {
    val names = domain.mailx.map(_.name).toList
    val name = checkHostName(host, names)
    val mx = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    (name._1 && mx._1 && unique._1, name._2 :: mx._2 :: unique._2 :: Nil)
  }
  
  def checkNSHost(host: NSHost, domain: ExtendedDomain) = {
    val names = domain.nameservers.map(_.name).toList
    val name = checkHostName(host, names)
    val ns = checkDomainName(host.hostname)
    val unique = isUnique(host.hostname, names)
    (name._1 && ns._1 && unique._1, name._2 :: ns._2 :: unique._2 :: Nil)
  }
  
  def checkPointerHost(host: PointerHost, domain: ExtendedDomain) = {
    val names = domain.pointer.map(_.name).toList
    val name = checkHostName(host, names)
    val ptr = checkDomainName(host.ptrdname)
    val unique = isUnique(host.ptrdname, names)
    (name._1 && ptr._1 && unique._1, name._2 :: ptr._2 :: unique._2 :: Nil)
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
    (name._1 && mname._1 && rname._1 && ttl._1 && refresh._1 && retry._1 && minimum._1 && expire._1,
     name._2 :: mname._2 :: rname._2 :: ttl._2 :: refresh._2 :: retry._2 :: minimum._2 :: expire._2 :: Nil)
  }
  
  def checkTxtHost(host: TxtHost, domain: ExtendedDomain) = {
    val names = domain.text.map(_.name).toList
    val name = checkHostName(host, names)
    if(host.strings != null && !host.strings.isEmpty) (name._1, name._2 :: Nil)
    else (false, validationMessages("empty").format("Text") :: Nil)
  }
  
  def checkGenericHost(host: Host, domain: ExtendedDomain) = {
    val names = domain.text.map(_.name).toList
    val name = checkHostName(host, names)
    (name._1, name._2 :: Nil)
  }
  
  def checkDomainName(name: String, absolute: Boolean = true, relative: Boolean = true) = {
    if (name == null)
      (false, validationMessages("required").format("domain name"))
    else if (name.length > 255)
      (false, validationMessages("name_long").format(name))
    else if (name.split("""\.""").exists(_.length > 63))
      (false, validationMessages("name_part_long").format(name))
    else if (!relative && absolute && name != "@" && name.lastIndexOf(".") != name.length - 1)
      (false, validationMessages("name_not_absolute").format(name))
    else if (relative && !absolute && name != "@" && name.lastIndexOf(".") == name.length - 1)
      (false, validationMessages("name_not_relative").format(name))
    else
      (true, null)
  }

  def checkHostName(host: Host, names: List[String]) = {
    val domainCheck = checkDomainName(host.name, false, true)
    if(!domainCheck._1) domainCheck
    else {
      val uniqueCheck = isUnique(host.name, names)
      if(!uniqueCheck._1) uniqueCheck
      else if (host.name == "@" || host.name.matches("""([a-zA-Z0-9\*]{1}([a-zA-Z0-9\-\*]*[a-zA-Z0-9\*]{1})*\.{0,1})*""")) (true, null)
      else (false, validationMessages("name_not_hostname").format(host.name))
    }
  }

  def isUnique(item: String, items: List[String], exclude: List[String] = Nil) =
    if (exclude.contains(item) || items.filter(_ == item).length <= 1) (true, null)
    else (false, validationMessages("duplicate").format(item))

  def checkTimeValue(time: String) =
    if (time.matches("""^([0-9]+[hdmswHDMSW]{0,1})+$""")) (true, null)
    else (false, validationMessages("ttl_not_valid").format(time))

  def checkUnreachable(domain: ExtendedDomain) = {
    val domainNames = DNSCache.getDomainNames

    def findName(domainNameParts: Array[String]): String =
      if (domainNameParts.isEmpty) domain.fullName
      else if (!domainNames.contains(domainNameParts.mkString(".") + "." + domain.fullName.substring(0, domain.fullName.length - 1)))
        findName(domainNameParts.tail)
      else domainNameParts.mkString(".") + "." + domain.fullName

    domain.hosts.foldLeft((true, List[String]())) { (status, host) =>
      val name = findName(host.name.split("""\.""").tail)
      val valid = name == domain.fullName
      val message =
        if (valid) null
        else validationMessages("unreachable").format(host.name, domain.fullName, name)
      (status._1 && valid, if (valid) status._2 else message :: status._2)
    }
  }

  def checkRecordDuplicities(domain: ExtendedDomain) = {
    val duplicities = domain.hosts.map { host =>
      RecordType.values.find(_.toString == host.typ) match {
        case None => null
        case Some(typ) => {
          val qname =
            if (host.name == domain.fullName || host.name == "@") domain.fullName
            else host.name + "." + domain.fullName

          DNSCache.findDomain(typ.id, qname.split("""\.""").toList) match {
            case None => null
            case Some(existingDomain) => {
              if (existingDomain.fullName == domain.fullName) null
              else {
                val hostname = qname.substring(0, qname.lastIndexOf(existingDomain.fullName) - 1)
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

    if (duplicities.isEmpty) (true, Nil)
    else (false, duplicities.map {
      case (path, typ, domain) =>
        validationMessages("duplicate_record").format(typ, path, domain.fullName)
    })
  }

  def reorganize(srcdomain: ExtendedDomain) = {
    val name = srcdomain.nameParts.toList

    @tailrec
    def ancestorDomains(parts: List[String], domains: List[(String, ExtendedDomain)] = List()): List[(String, ExtendedDomain)] = {
      if (parts.isEmpty) domains
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
              DNSCache.setDomain(newdomain)
              JsonIO.storeDomain(newdomain)
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
    DNSCache.getDomains.foreach {
      case (extension, domains) =>
        domains.foreach {
          case (name, (timestamp, domain)) =>
            val newdomain = reorganize(domain)
            if (newdomain.head.hosts.length != domain.hosts.length) JsonIO.storeDomain(domain)
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
    "incomplete" -> "The domain is missing required records")
}