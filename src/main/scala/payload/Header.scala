package payload

import scala.collection.immutable.BitSet
import org.jboss.netty.buffer.ChannelBuffer

class Header(buf: ChannelBuffer) {
  
  
	lazy val MIN_USHORT = 0;
	lazy val MAX_USHORT = 0xFFFF;
	lazy val FLAGS_QR = 15;
	lazy val FLAGS_OPCODE = 11;
	lazy val FLAGS_AA = 10;
	lazy val FLAGS_TC = 9;
	lazy val FLAGS_RD = 8;
	lazy val FLAGS_RA = 7;
	lazy val FLAGS_Z = 4;
	lazy val FLAGS_RCODE = 0;
  
	
	object OP_CODE extends Enumeration {
	   val QUERY = 0
	   val IQUERY = 1
	   val STATUS = 2
	}
  
	
	// TODO: VERIFY that the bits are 16 !!
	
	  
	  val id = buf.readUnsignedShort
	  val flagsInt = buf.readUnsignedShort
	  
	  // FLAGS
	  val qr = shiftBits(flagsInt, FLAGS_QR, 0x1) != 0 // boolean
	  val opcode = shiftBits(flagsInt, FLAGS_OPCODE, 0xF) // int
	  val aa = shiftBits(flagsInt, FLAGS_AA, 0x1) != 0 // boolean
	  val tc = shiftBits(flagsInt, FLAGS_TC, 0x1) != 0 // boolean
	  val rd = shiftBits(flagsInt, FLAGS_RD, 0x1) != 0 // boolean
	  val ra = shiftBits(flagsInt, FLAGS_RA, 0x1) != 0 // boolean
	  val z = shiftBits(flagsInt,FLAGS_Z,0x7) // always zero
	  val rcode = shiftBits(flagsInt, FLAGS_RCODE, 0xF) // int
	  
	  //-----
	  
	  
	  
	  val qdcount = buf.readUnsignedShort
	  val ancount = buf.readUnsignedShort
	  val nscount = buf.readUnsignedShort
	  val arcount = buf.readUnsignedShort
	  println("Parsing of the Header: DONE")
	  
	  
	
	
	def shiftBits(n: Int,shift: Int, mask: Int) = {
	  (n >> shift) & mask
	}
	
	
  /* *
   * * WARNING !!!
   * * All variables are initialized with a NON-PERMITTED value !
   * *
   */
  
  // ID: A 16 bit identifier assigned by the program that
  // generates any kind of query.  This identifier is copied
  // the corresponding reply and can be used by the requester
  // to match up replies to outstanding queries.
  //var id = -1
  
  //var flags = -1
  
  // QR: A 1 bit field that specifies whether this message is a
  // query (0), or a response (1).
  //var qr = -1
  
  // OPCODE: A 4 bit field that specifies kind of query in this
  // message.  This value is set by the originator of a query
  // and copied into the response.  The values are:
  // 0               a standard query (QUERY)
  // 1               an inverse query (IQUERY)
  // 2               a server status request (STATUS)
  // 3-15            reserved for future use  
  // corresponds to the name which matches the query name, or
  // the first owner name in the answer section.
  //var opcode = -1
  
  // AA: Authoritative Answer - this bit is valid in responses,
  // and specifies that the responding name server is an
  // authority for the domain name in question section.
  // Note that the contents of the answer section may have
  // multiple owner names because of aliases.  The AA bit  
  //var aa = -1
  
  // TC: TrunCation - specifies that this message was truncated
  // due to length greater than that permitted on the
  // transmission channel.
  //var tc = -1
  
  // RD: Recursion Desired - this bit may be set in a query and
  // is copied into the response.  If RD is set, it directs
  // the name server to pursue the query recursively.
  // Recursive query support is optional.
  //var rd = -1 		// 1 bit
  
  // RA: Recursion Available - this be is set or cleared in a
  // response, and denotes whether recursive query support is
  // available in the name server.
  //var ra = -1
  
  // Z: Reserved for future use.  Must be zero in all queries
  // and responses.
  //val z = 0		 		// 1 bit
  
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
  //var rcode = -1		// 4 bit
  
  // QDCOUNT: an unsigned 16 bit integer specifying the number of
  // entries in the question section.
  //var qdcount = -1
  

  // ANCOUNT: an unsigned 16 bit integer specifying the number of
  // resource records in the answer section.
  //var ancount = -1
  

  // NSCOUNT: an unsigned 16 bit integer specifying the number of name
  // server resource records in the authority records
  // section.
  //var nscount = -1

  // ARCOUNT: an unsigned 16 bit integer specifying the number of
  // resource records in the additional records section.
  //var arcount = -1

}