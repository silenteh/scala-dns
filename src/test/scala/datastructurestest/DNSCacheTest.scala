/*******************************************************************************
 * Copyright 2012 silenteh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package datastructurestest
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import datastructures.DNSCache
import models.ExtendedDomain
import models.ExtendedDomain
import scala.collection.immutable.TreeMap
import scala.collection.immutable.Map
import org.scalatest.matchers._
import org.scalatest.matchers.ShouldMatchers

class DNSCacheTest extends FunSpec with BeforeAndAfter with ShouldMatchers{

  val cache = DNSCache  
  
  describe("DNS Cache") {

    // check if the cache object is initialized correctly
    it("should return None for the root domain: .") {     
      val result = cache.getDomain(".","")
      assert(result === None)      
    }
    
    // check if the adding of a domain name works
    it("should allow the insertion of an entry to the cache and return the map just added") {     
      val result = cache.setDomain(new ExtendedDomain("com","example",500))
      result should not be ("empty")
      result should not be (None)
      result should contain key ("example")
      result.get("example") should not be (null)
      result.get("example").get should not be (None)
      result.get("example").get._2 should have (
    		  'extension ("com"),
    		  'name ("example"),
    		  'ttl (500)
    		  )      
    }
  }
  
}