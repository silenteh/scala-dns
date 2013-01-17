/**
 * *****************************************************************************
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
 * ****************************************************************************
 */
package server.dns

import datastructures.DNSCache
import models.ExtendedDomain
import models.Host
import models.CnameHost
import enums.RecordType
import datastructures.DomainNotFoundException
import scala.annotation.tailrec
import records._
import org.slf4j.LoggerFactory
import scala.Array.canBuildFrom
import scala.annotation.tailrec

object DnsLookupService {
  val logger = LoggerFactory.getLogger("app")

  def hostToRecords(qname: List[String], qtype: Int, qclass: Int): List[(String, AbstractRecord)] = {
    val domain = DNSCache.getDomain(qtype, qname)
    filterDuplicities(qname.mkString(".") + ".", domain.getHosts(relativeHostName(qname, domain))
      .filter(h => qtype == RecordType.ALL.id || h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString)
      .map { host =>
        val usedCnames = initUsedCnames(host, qname)
        resolveHost(domain, host, qtype, usedCnames, List(), domain)
      }).flatten
  }

  // Queries for ancestors of a specified host name, stops when the first match is found, 
  // e.g. for www.example.com host name the example.com and com host names would be examined.
  // When a "wildcards" parameter is set to true, all queries are prefixed with "*".
  def ancestorToRecords(domain: ExtendedDomain, qname: List[String], qtype: Int, qclass: Int, wildcards: Boolean) = {
	val names = findAncestors(domain, qname, qclass, wildcards)
	names.foldRight(List[(String, AbstractRecord)]()) { case(name, records) =>
	  if(!records.isEmpty) records
	  else records ++ hostToRecords(name, qtype, qclass).map{ case (n, r) =>
	    (n.replace("""*""", qname.take(qname.indexOfSlice(n.split("""\.""").filterNot(_ == "*"))).mkString(".")), r)
	  }
	}
  }
  
  // Prepares output for zone file transfer.
  def zoneToRecords(qname: List[String], qclass: Int) = {
    val domain = DNSCache.getDomain(RecordType.ALL.id, qname)
    logger.debug(qname.mkString(".") + "., " + relativeHostName(qname, domain))
    val (hostnames, fullNameExists) = {
      val fullMatches = domain.hosts
        .filter(host => relativeHostName(qname, domain) == domain.fullName || (host.name.endsWith(relativeHostName(qname, domain))))
        .map(host => (host.name.split("""\.""") ++ domain.nameParts).toList).distinct

      if (!fullMatches.isEmpty && domain.hosts.exists(h => 
        relativeHostName(h.name.split("""\.""").toList, domain) == relativeHostName(qname, domain))) (fullMatches, true)
      else (findAncestors(domain, qname, qclass, true), false)
    }

    val records = hostnames.foldRight(Set[List[(String, AbstractRecord)]]()) { case (name, records) =>
      if(!fullNameExists && !records.isEmpty && records.size > 0) records
      else
        records + RecordType.values.filter(t => t.id != 5 && t.id < 252).foldRight(List[(String, AbstractRecord)]()) {
          case (typ, record) =>
            if(fullNameExists) record ++ hostToRecords(name.toList, typ.id, qclass)
            else record ++ hostToRecords(name.toList, typ.id, qclass).map{ case (n, r) => 
              (n.replace("""*""", qname.take(qname.indexOfSlice(n.split("""\.""").filterNot(_ == "*"))).mkString(".")), r)
	        }
      }
    }
    
    /*val records = RecordType.values.filter(typ => typ.id != 5 && typ.id < 252).map {typ =>
      hostnames.foldRight(Set[List[(String, AbstractRecord)]]()) { case (name, records) =>
        if(!fullNameExists && !records.isEmpty) records
        else if (fullNameExists) records + hostToRecords(name.toList, typ.id, qclass)
        else records + hostToRecords(name.toList, typ.id, qclass).map{ case (n, r) => 
          (n.replace("""*""", qname.take(qname.indexOfSlice(n.split("""\.""").filterNot(_ == "*"))).mkString(".")), r)
	    }
      }
    }.foldRight(Set[List[(String, AbstractRecord)]]()) { case(left, right) => left ++ right }*/
    
    val soaExists = records.flatten.exists(_._2 match {
      case r: SOA => true
      case _ => false
    })

    if (!soaExists) List()
    else toDistinct(records.foldRight(List[(String, AbstractRecord)]()) { case (left, right) => 
      left.filterNot(record => right.exists(r => r._1 == record._1 && r._2.isEqualTo(record._2))) ++ right
    }.toList)
  }

  // Not a tail recursion
  private def resolveHost(
    domain: ExtendedDomain,
    host: Host,
    qtype: Int,
    usedCnames: List[String],
    shownCnames: List[(String, Array[AbstractRecord])],
    oldDomain: ExtendedDomain,
    records: Array[(String, AbstractRecord)] = Array()
  ): Array[(String, AbstractRecord)] =
    host match {
      case host: CnameHost =>
        if (!usedCnames.contains(absoluteHostName(host.hostname, domain.fullName))) {
          try {
            val (qname, newDomain, newHost) =
              if (host.hostname.contains("@")) {
                val qname = oldDomain.nameParts.toList
                (qname, oldDomain, host.changeHostname(qname.mkString(".") + "."))
              } else {
                val qname = absoluteHostName(host.hostname, domain.fullName).split("""\.""").toList
                (qname, DNSCache.getDomain(qtype, qname), host)
              }

            records ++ newDomain.getHosts(relativeHostName(qname, newDomain))
              .filter(h => h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString)
              .map {
                val absoluteCname = absoluteHostName(newHost.hostname, newDomain.fullName)
                val absoluteHostname = absoluteHostName(newHost.name, oldDomain.fullName)
                resolveHost(domain, _, qtype, absoluteCname :: usedCnames, (absoluteHostname, newHost.toRData) :: shownCnames, newDomain)
              }.flatten
          } catch {
            // Cname points to an external domain, search cache
            // Add the last internal result to the Cnames only if the host name is resolved
            case ex: DomainNotFoundException =>
              records ++ recordsToFlatArray(shownCnames.reverse) ++ host.toRData.map((absoluteHostName(host.name, oldDomain.fullName), _))
          }
        } else {
          logger.error("Infinite loop when resolving a CNAME: " + usedCnames.reverse.mkString(" -> ") + " -> " + host.hostname)
          records
        }
      case _ => {
        //if(host.typ == RecordType.SOA.toString && !shownCnames.isEmpty && )
        val absname = absoluteHostName(host.name, oldDomain.fullName)
        records ++ recordsToFlatArray(shownCnames.reverse) ++ host.toRData.map((absname, _))
      }
    }
  
  private def findAncestors(domain: ExtendedDomain, qname: List[String], qclass: Int, wildcards: Boolean) = {
    val name = qname.take(qname.indexOfSlice(domain.nameParts))

    if (name.size + domain.nameParts.size <= 1) List()
    else {
      @tailrec
      def findHost(qname: List[String], names: List[List[String]]): List[List[String]] =
        if (qname.isEmpty && wildcards) ("*" :: domain.nameParts.toList) :: names
        else if (qname.isEmpty) (domain.nameParts.toList) :: names
        else if (wildcards) findHost(qname.tail, ("*" :: qname ++ domain.nameParts) :: names)
        else findHost(qname.tail, (qname ++ domain.nameParts) :: names)
      
      findHost(name.tail, List())
    }
  }
  
  private def relativeHostName(qname: List[String], domain: ExtendedDomain) = {
    val hnm = qname.take(qname.indexOfSlice(domain.nameParts)).mkString(".")
    if (hnm.length == 0 || hnm == "@") domain.fullName else hnm
  }

  private def absoluteHostName(name: String, basename: String) =
    if (name == "@") basename
    else if (name.endsWith(".")) name
    else name + "." + basename

  private def recordsToFlatArray[T](records: List[(String, Array[T])]) =
    records.map { case (name, value) => value.map((name, _)) }.flatten.toArray

  private def initUsedCnames(host: Host, qname: List[String]) =
    host match {
      case h: CnameHost => List(qname.mkString(".") + ".")
      case _ => List[String]()
    }

  private def filterDuplicities(qname: String, records: List[Array[(String, AbstractRecord)]]) = {
    val closestSoa = {
      val soas = records.flatten.filter(_._2.isInstanceOf[SOA])
      if(soas.isEmpty) null else soas.minBy(r => qname.indexOf(r._1))._2
    }
    
    val filteredRecords = records.map { record =>
      val isDuplicateEntry = record.exists {
        case (name, value) => records
          .filterNot(_.deep == record.deep).exists(_.exists(r => r._1 == name && r._2.isInstanceOf[CNAME]))
      }
      
      if (!isDuplicateEntry) record
      else record.filter(_._2 match {
        case r: CNAME => !record.exists(!_._2.isInstanceOf[CNAME])
        case r: SOA => r.isEqualTo(closestSoa)
        case _ => true
      }).map(r => (qname, r._2))
    }

    if (filteredRecords.map(_.filter(_._2.isInstanceOf[A])).flatten.distinct.length > 1) filteredRecords
    else filteredRecords
  }

  @tailrec
  private def toDistinct(record: List[(String, AbstractRecord)], result: List[(String, AbstractRecord)] = List()): List[(String, AbstractRecord)] =
    if (record.isEmpty) result
    else if ((record.head._2.description != RecordType.A.toString && record.head._2.description != RecordType.AAAA.toString) &&
      result.exists(r => r._1 == record.head._1 && r._2.isEqualTo(record.head._2))) toDistinct(record.tail, result)
    else toDistinct(record.tail, record.head :: result)
}