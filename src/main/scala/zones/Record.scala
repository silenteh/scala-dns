package zones
import enums.RecordType

case class Record(domainName: String, rtype: Int, ttl: Int, data: String)