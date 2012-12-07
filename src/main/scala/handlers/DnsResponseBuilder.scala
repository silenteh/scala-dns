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
    
    domain.getHosts(relativeHostName(qname, domain))
      .filter(h => h.typ == RecordType(qtype).toString || h.typ == RecordType.CNAME.toString)
      .map { host =>
        val usedCnames = initUsedCnames(host, qname)
        resolveHost(domain, host, qtype, usedCnames, domain)
      }.flatten
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
    usedCnames: Map[String, Array[AbstractRecord]],
    oldDomain: ExtendedDomain,
    records: Array[(String, AbstractRecord)] = Array()
  ): Array[(String, AbstractRecord)] =
    host match {
      case host: CnameHost =>
        if (!usedCnames.contains(host.hostname)) {
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
                val absname = absoluteHostName(newHost.hostname, newDomain.fullName)
                resolveHost(domain, _, qtype, usedCnames + (absname -> newHost.toRData), newDomain)
              }.flatten
          } catch {
            // Cname points to an external domain, search cache
            // Add the last internal result to the Cnames only if the host name is resolved
            case ex: DomainNotFoundException => records ++ host.toRData.map((host.hostname, _))
          }
        } else {
          logger.error("Infinite loop when resolving a CNAME: " + usedCnames.keys.mkString(" -> ") + " -> " + host.hostname)
          records
        }
      case _ => {
        val absname = absoluteHostName(host.name, domain.fullName)
        records ++ recordsToFlatArray(usedCnames) ++ host.toRData.map((absname, _))
      }
    }
  
  private def relativeHostName(qname: List[String], domain: ExtendedDomain) = {
    val hnm = qname.take(qname.indexOfSlice(domain.nameParts)).mkString(".")
    if (hnm.length == 0 || hnm == "@") domain.fullName else hnm
  }
  
  private def absoluteHostName(name: String, basename: String) = 
    if (name == "@") basename
    else if (name.contains(basename)) name
    else name + "." + basename
    
  private def recordsToFlatArray[T](records: Map[String, Array[T]]) = 
    records.map {case(name, value) => value.map((name, _))}.flatten.toArray
    
  private def initUsedCnames(host: Host, qname: List[String]) = 
    host match {
      case h: CnameHost => Map(qname.mkString(".") + "." -> host.toRData)
      case _ => Map[String, Array[AbstractRecord]]()
    }
}