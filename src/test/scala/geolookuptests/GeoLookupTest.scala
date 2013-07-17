package geolookuptests
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import countries.Lookup
import com.maxmind.geoip2.model.City
import enums.ContinentsEnum

class GeoLookupTest extends FunSpec with BeforeAndAfter with ShouldMatchers{

  
  describe("Geo Lookup") {
    
    it("should return the correct continent resoltution for a bunch of hard coded IPs") {
      val ip = "94.74.231.214"
      val city = Lookup.ipToCity(ip)                  
      assert(city != null)
      val country = Lookup.ipToCountry(ip)
      assert(country != null)
      val continent = Lookup.ipToContinent(ip)
      assert(continent != null)
      assert(continent == ContinentsEnum.europe)
    }
    
    
  }
  
}