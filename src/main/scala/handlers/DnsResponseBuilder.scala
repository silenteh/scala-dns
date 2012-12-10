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
package handlers

import datastructures.DNSCache
import models.ExtendedDomain
import models.Host
import models.CnameHost
import enums.RecordType
import datastructures.DomainNotFoundException
import scala.annotation.tailrec
import payload.RRData
import records._
import org.slf4j.LoggerFactory

object DnsResponseBuilder {
  val logger = LoggerFactory.getLogger("app")

  def hostToRecords(qname: List[String], qtype: Int, qclass: Int): List[(String, AbstractRecord)] = {
    val domain = DNSCache.getDomain(qtype, qname)
    filterDuplicities(qname.mkString(".") + ".", domain.getHosts(relativeHostName(qname, domain))
      .filter(h => h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString)
      .map { host =>
        val usedCnames = initUsedCnames(host, qname)
        resolveHost(domain, host, qtype, usedCnames, List(), domain)
      }).flatten
  }

  // Queries for ancestors of a specified host name, stops when the first match is found, 
  // e.g. for www.example.com host name the example.com and com host names would be examined.
  // When a "wildcards" parameter is set to true, all queries are prefixed with "*".
  def ancestorToRecords(domain: ExtendedDomain, qname: List[String], qtype: Int, qclass: Int, wildcards: Boolean) = {
    val name = qname.take(qname.indexOfSlice(domain.nameParts))

    if (name.size + domain.nameParts.size <= 1) List()
    else {

      @tailrec
      def findHost(qname: List[String], name: String): List[(String, AbstractRecord)] = 
        if (qname.isEmpty && wildcards) hostToRecords("*" :: domain.nameParts.toList, qtype, qclass)
        else if (qname.isEmpty) hostToRecords("*" :: domain.nameParts.toList, qtype, qclass)
        else if (wildcards) {
          val rdata = hostToRecords("*" :: qname ++ domain.nameParts, qtype, qclass)
          if (!rdata.isEmpty) rdata.map { case (n, r) => (n.replace("""*""", name), r) }
          else findHost(qname.tail, name + "." + qname.head)
        } else {
          val rdata = hostToRecords(qname ++ domain.nameParts, qtype, qclass)
          if (!rdata.isEmpty) rdata
          else findHost(qname.tail, name + "." + qname.head)
        }
      
      findHost(name.tail, name.head)
    }
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
              if(host.hostname.contains("@")) {
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
        val absname = absoluteHostName(host.name, oldDomain.fullName)
        records ++ recordsToFlatArray(shownCnames.reverse) ++ host.toRData.map((absname, _))
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
    records.map {case(name, value) => value.map((name, _))}.flatten.toArray
    
  private def initUsedCnames(host: Host, qname: List[String]) = 
    host match {
      case h: CnameHost => List(qname.mkString(".") + ".")
      case _ => List[String]()
    }
    
  private def filterDuplicities(qname: String, records: List[Array[(String, AbstractRecord)]]) = {
    val filteredRecords = records.map {record => 
      val isDuplicateEntry = record.exists {case (name, value) => records
        .filterNot(_.deep == record.deep).exists(_.exists(r => r._1 == name && r._2.isInstanceOf[CNAME]))} 
      
      if(!isDuplicateEntry) record
      else record.filter(_._2 match {
        case r: CNAME => !record.exists(!_._2.isInstanceOf[CNAME])
        case _ => true
      }).map(r => (qname, r._2))
    }
    
    if(filteredRecords.map(_.filter(_._2.isInstanceOf[A])).flatten.distinct.length > 1) filteredRecords
    else filteredRecords.map(_.distinct)
  }
}