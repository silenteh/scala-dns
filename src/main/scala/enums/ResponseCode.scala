package enums

object ResponseCode extends Enumeration {
  // 0 : No error condition
  val OK = Value(0)
  
  // 1 : Format error - The name server was unable to interpret the query.
  val FORMAT_ERROR = Value(1)
  
  // 2 : Server failure - The name server was unable to process this query due to a problem with the name server.
  val SERVER_FAILURE = Value(2)
  
  // 3 : Name Error - Meaningful only for responses from an authoritative name server, this code signifies that the
  // domain name referenced in the query does not exist.
  val NAME_ERROR = Value(3)
  
  // 4 : Not Implemented - The name server does not support the requested kind of query.
  val NOT_IMPLEMENTED = Value(4)
  
  // 5 : Refused - The name server refuses to perform the specified operation for policy reasons.  For example, a name server
  // may not wish to provide the information to the particular requester, or a name server may not wish to perform a particular operation
  val REFUSED = Value(5)
}