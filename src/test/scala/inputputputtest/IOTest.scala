package inputputputtest

import scala.collection.Traversable
import scala.reflect.Manifest
import scala.collection.immutable.List
import java.util.Collection
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import domainio.JsonIO
import datastructures.DNSCache
import models.SoaHost
import models.NSHost
import models.AddressHost
import models.ExtendedDomain
import models.WeightedIP
import models.WeightedNS
import java.net.URLDecoder
import datastructures.DNSAuthoritativeSection

class IOTest extends FunSpec with BeforeAndAfter with ShouldMatchers {
  
  val applicationRoot = "/" + new File("").getAbsolutePath.replace("""\""", "/")
  val path = 
    "/" + URLDecoder.decode(this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath + "data/", "UTF-8").substring(applicationRoot.length + 1)
  val domainJsons = Map(
    "example.com." -> "{\"origin\":\"example.com.\",\"ttl\":86400,\"SOA\":[{\"class\":\"in\",\"at\":\"1D\",\"mname\":\"ns1.example.com.\",\"rname\":\"hostmaster.example.com.\",\"serial\":\"2002022401\",\"refresh\":\"3H\",\"retry\":\"15\",\"expire\":\"1w\",\"minimum\":\"3h\"}],\"NS\":[{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns1.example.com.\"}]},{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns2.smokeyjoe.com.\"}]}],\"MX\":[{\"class\":\"in\",\"priority\":10,\"value\":\"mail.another.com\"}],\"A\":[{\"class\":\"in\",\"name\":\"ns1\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.1\"}]},{\"class\":\"in\",\"name\":\"www\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.2\"}]},{\"class\":\"in\",\"name\":\"bill\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.3\"}]},{\"class\":\"in\",\"name\":\"fred\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.4\"}]}],\"cname\":[{\"class\":\"in\",\"name\":\"ftp\",\"value\":\"www.example.com.\"}]}",
    "example.net." -> "{\"origin\":\"example.net.\",\"ttl\":86400,\"SOA\":[{\"class\":\"in\",\"at\":\"1D\",\"mname\":\"ns1.example.net.\",\"rname\":\"hostmaster.example.net.\",\"serial\":\"2002022401\",\"refresh\":\"3H\",\"retry\":\"15\",\"expire\":\"1w\",\"minimum\":\"3h\"}],\"NS\":[{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns1.example.net.\"}]},{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns2.smokeyjoe.com.\"}]}],\"MX\":[{\"class\":\"in\",\"priority\":10,\"value\":\"mail.another.com\"}],\"A\":[{\"class\":\"in\",\"name\":\"ns1\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.1\"}]},{\"class\":\"in\",\"name\":\"www\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.2\"}]},{\"class\":\"in\",\"name\":\"bill\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.3\"}]},{\"class\":\"in\",\"name\":\"fred\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.4\"}]}],\"cname\":[{\"class\":\"in\",\"name\":\"ftp\",\"value\":\"www.example.net.\"}]}",
    "example2.com." -> "{\"origin\":\"example2.com.\",\"ttl\":86400,\"SOA\":[{\"class\":\"in\",\"at\":\"1D\",\"mname\":\"ns1.example2.com.\",\"rname\":\"hostmaster.example2.com.\",\"serial\":\"2002022401\",\"refresh\":\"3H\",\"retry\":\"15\",\"expire\":\"1w\",\"minimum\":\"3h\"}],\"NS\":[{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns1.example2.com.\"}]},{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns2.smokeyjoe.com.\"}]}],\"MX\":[{\"class\":\"in\",\"priority\":10,\"value\":\"mail.another.com\"}],\"A\":[{\"class\":\"in\",\"name\":\"ns1\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.1\"}]},{\"class\":\"in\",\"name\":\"www\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.2\"}]},{\"class\":\"in\",\"name\":\"bill\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.3\"}]},{\"class\":\"in\",\"name\":\"fred\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.4\"}]}],\"cname\":[{\"class\":\"in\",\"name\":\"ftp\",\"value\":\"www.example2.com.\"}]}",
    "example2.net." -> "{\"origin\":\"example2.net.\",\"ttl\":86400,\"SOA\":[{\"class\":\"in\",\"at\":\"1D\",\"mname\":\"ns1.example2.net.\",\"rname\":\"hostmaster.example2.net.\",\"serial\":\"2002022401\",\"refresh\":\"3H\",\"retry\":\"15\",\"expire\":\"1w\",\"minimum\":\"3h\"}],\"NS\":[{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns1.example2.net.\"}]},{\"class\":\"in\",\"value\":[{\"weight\":1,\"ns\":\"ns2.smokeyjoe.com.\"}]}],\"MX\":[{\"class\":\"in\",\"priority\":10,\"value\":\"mail.another.com\"}],\"A\":[{\"class\":\"in\",\"name\":\"ns1\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.1\"}]},{\"class\":\"in\",\"name\":\"www\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.2\"}]},{\"class\":\"in\",\"name\":\"bill\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.3\"}]},{\"class\":\"in\",\"name\":\"fred\",\"value\":[{\"weight\":1,\"ip\":\"192.168.0.4\"}]}],\"cname\":[{\"class\":\"in\",\"name\":\"ftp\",\"value\":\"www.example2.net.\"}]}"
  )
  
  val pathFile = new File(applicationRoot + path)
  pathFile.mkdirs
  
  domainJsons.foreach {case(name, json) =>
    val fos = new BufferedOutputStream(new FileOutputStream(new File(applicationRoot + path + name + "json")))
    fos.write(json.getBytes)
    fos.close
  }
  
  describe("JSON writer") {
	it("should read all json files from a specified folder into ExtendedDomain objects") {
	  val domains = JsonIO.loadDataOfType(pathFile, classOf[ExtendedDomain]) { DNSAuthoritativeSection.setDomain(_) }
	  
	  val examplecom = DNSCache.findDomain(1, "com", "example")
	  //val example2com = DNSCache.getDomain("com", "example2")
	  //val examplenet = DNSCache.getDomain("net", "example")
	  //val example2net = DNSCache.getDomain("net", "example2")
	  examplecom should not be(None)
	  examplecom.get should have(
	    'extension ("com"),
	    'name ("example"),
	    'ttl (86400)
	  )
	  
	  DNSCache.removeDomain("com", "example")
	  DNSCache.removeDomain("com", "example2")
	  DNSCache.removeDomain("net", "example")
	  DNSCache.removeDomain("net", "example2")
	  
	  /*example2com should not be(None)
	  example2com.get should have(
	    'extension ("com"),
	    'name ("example2"),
	    'ttl (86400)
	  )
	  
	  examplenet should not be(None)
	  examplenet.get should have(
	    'extension ("net"),
	    'name ("example"),
	    'ttl (86400)
	  )
	  
	  example2net should not be(None)
	  example2net.get should have(
	    'extension ("net"),
	    'name ("example2"),
	    'ttl (86400)
	  )*/
	}
	
	it("should write a json file from an ExtendedDomain object") {
	  val soa = Array(new SoaHost("in", "1", "@", "ns1.testexample.com", "ns2.testexample.com", "123456789", "1", "1", "1", "1"))
	  val nsHosts = Array(new NSHost("in", "@", Array(new WeightedNS(10, "ns1.testexample.com"))), new NSHost("in", "@", Array(new WeightedNS(5, "ns2.testexample.com"))))
	  val addressHosts = Array(new AddressHost("in", "host1.testexample.com", Array(new WeightedIP(1, "10.0.0.1"))), new AddressHost("in", "host2.testexample.com", Array(new WeightedIP(1, "10.0.0.2"), new WeightedIP(1, "10.0.0.3"))))
	  val domain = new ExtendedDomain("testexample.com.", 500, nsHosts, soa, null, addressHosts, null, null, null, null, null)
	  
	  pathFile.listFiles.find(file => file.getName == domain.fullName + "json") should be(None)
	  
	  val clearPath = path.substring(0, path.length - 1)
	  
	  JsonIO.storeData(domain, domain.getFilename, clearPath)
	  
	  val domainFile = pathFile.listFiles.find(file => file.getName == domain.fullName + "json")
	  domainFile should not be(None)
	  domainFile.get.exists should be(true)
	  domainFile.get.delete
	}
  }

  pathFile.deleteOnExit
}