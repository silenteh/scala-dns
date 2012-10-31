package models

class Domain(extension: String, name: String, ttl: Long, nameservers: List[Host] = List.empty[Host]) {
  
  
  val TTL = ttl
  
  def fullName() = {
    val fn = name + "." + extension
    fn
  }
  
  def reverseFullName() = {
    val fn = extension + "." + name
    fn
  }
  
  

}