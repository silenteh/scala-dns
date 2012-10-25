package payload

import scala.collection.immutable.BitSet

class Header {
  
  
  /* *
   * * WARNING !!!
   * * All variables are initialized with a NON-PERMITTED value !
   * *
   */
  
  // ID: A 16 bit identifier assigned by the program that
  // generates any kind of query.  This identifier is copied
  // the corresponding reply and can be used by the requester
  // to match up replies to outstanding queries.
  var id = -1 
  
  // QR: A 1 bit field that specifies whether this message is a
  // query (0), or a response (1).
  var qr = false
  
  // OPCODE: A 4 bit field that specifies kind of query in this
  // message.  This value is set by the originator of a query
  // and copied into the response.  The values are:
  // 0               a standard query (QUERY)
  // 1               an inverse query (IQUERY)
  // 2               a server status request (STATUS)
  // 3-15            reserved for future use  
  // corresponds to the name which matches the query name, or
  // the first owner name in the answer section.
  var opcode = BitSet(-1,-1,-1,-1)
  
  // AA: Authoritative Answer - this bit is valid in responses,
  // and specifies that the responding name server is an
  // authority for the domain name in question section.
  // Note that the contents of the answer section may have
  // multiple owner names because of aliases.  The AA bit  
  var aa = BitSet(-1)
  
  // TC: TrunCation - specifies that this message was truncated
  // due to length greater than that permitted on the
  // transmission channel.
  var tc = BitSet(-1)
  
  // RD: Recursion Desired - this bit may be set in a query and
  // is copied into the response.  If RD is set, it directs
  // the name server to pursue the query recursively.
  // Recursive query support is optional.
  var rd = BitSet(-1) 		// 1 bit
  
  // RA: Recursion Available - this be is set or cleared in a
  // response, and denotes whether recursive query support is
  // available in the name server.
  var ra = BitSet(-1)
  
  // Z: Reserved for future use.  Must be zero in all queries
  // and responses.
  val z = 0		 		// 1 bit
  
  // RCODE: Response code - this 4 bit field is set as part of
  // responses.  The values have the following
  // interpretation:
  //
  //    0               No error condition
  //
  //    1               Format error - The name server was
  //                    unable to interpret the query.
  //
  //    2               Server failure - The name server was
  //                    unable to process this query due to a
  //                    problem with the name server.
  //
  //    3               Name Error - Meaningful only for
  //                    responses from an authoritative name
  //                    server, this code signifies that the
  //                    domain name referenced in the query does
  //                    not exist.
  //
  //    4               Not Implemented - The name server does
  //                    not support the requested kind of query.
  //
  //    5               Refused - The name server refuses to
  //                    perform the specified operation for
  //                    policy reasons.  For example, a name
  //                    server may not wish to provide the
  //                    information to the particular requester,
  //                    or a name server may not wish to perform
  //                    a particular operation (e.g., zone
  //					transfer) for particular data.
  //	6-15            Reserved for future use.  
  var rcode = BitSet(-1,-1,-1,-1)		// 4 bit
  
  // QDCOUNT: an unsigned 16 bit integer specifying the number of
  // entries in the question section.
  var qdcount = -1
  

  // ANCOUNT: an unsigned 16 bit integer specifying the number of
  // resource records in the answer section.
  var ancount = -1
  

  // NSCOUNT: an unsigned 16 bit integer specifying the number of name
  // server resource records in the authority records
  // section.
  var nscount = -1

  // ARCOUNT: an unsigned 16 bit integer specifying the number of
  // resource records in the additional records section.
  var arcount = -1

}