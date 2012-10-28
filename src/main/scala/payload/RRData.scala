package payload

class RRData {

  // NAME            a domain name to which this resource record pertains.
  var name = ""
    
  // TYPE         two octets containing one of the RR type codes.  This
  //              field specifies the meaning of the data in the RDATA
  //              field.  
  var rtype= ""
  
    
  // CLASS        two octets which specify the class of the data in the
  //              RDATA field.
  var rclass = ""
    
    
  // TTL          a 32 bit unsigned integer that specifies the time
  //              interval (in seconds) that the resource record may be
  //              cached before it should be discarded.  Zero values are
  //              interpreted to mean that the RR can only be used for the
  //              transaction in progress, and should not be cached.
  var ttl = 0
  
  
  // RDLENGTH     an unsigned 16 bit integer that specifies the length in
  //              octets of the RDATA field.
  var rdlenght = 0
  
  // RDATA        a variable length string of octets that describes the
  //              resource.  The format of this information varies
  //              according to the TYPE and CLASS of the resource record.
  //              For example, the if the TYPE is A and the CLASS is IN,
  //              the RDATA field is a 4 octet ARPA Internet address.
  var rdata = ""
}