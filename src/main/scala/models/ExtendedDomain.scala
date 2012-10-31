package models

class ExtendedDomain(extension: String, name: String, ttl: Long, 
    nameservers: List[Host] = List.empty[Host], hosts: List[Host] = List.empty[Host])  
    extends Domain(extension,name,ttl,nameservers) {
  
  
  
  
}