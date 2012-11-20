package zones
import enums.RecordType

case class Record(val domainName: String, val rtype: Int, val ttl: Int, val data: String) {}