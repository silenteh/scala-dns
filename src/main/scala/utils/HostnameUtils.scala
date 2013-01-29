package utils

import models.ExtendedDomain

object HostnameUtils {
  def relativeHostName(qname: List[String], domain: ExtendedDomain) = {
    val hnm = qname.take(qname.lastIndexOfSlice(domain.nameParts)).mkString(".")
    if (hnm.length == 0 || hnm == "@") domain.fullName else hnm
  }

  def absoluteHostName(name: String, basename: String) = 
    if (name == "@") basename
    else if (name == null || name.endsWith(".")) name
    else name + "." + basename
}