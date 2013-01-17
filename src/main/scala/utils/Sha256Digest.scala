package utils

import java.security.MessageDigest
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

object Sha256Digest {
  def apply(string: String) = {
	val md = MessageDigest.getInstance("SHA-256")
	new HexBinaryAdapter().marshal(md.digest(string.getBytes))
  }
}