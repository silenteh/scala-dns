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
package countries

import enums.ContinentsEnum
import com.maxmind.geoip2.DatabaseReader
import java.io.File
import scala.io.Source
import java.net.InetAddress
import com.maxmind.geoip2.model.City

object Lookup {
  
  val unknown = "unknown"
  val europe = "europe"
  val northAmerica = "north-america"
  val southAmerica = "south-america"
  val asia = "asia"
  val africa = "africa"
  val oceania = "oceania"
  val antarctica = "antarctica"
    
  
  // initialize the db reader by loading the file from the resources  
  lazy val dbReader = new DatabaseReader(new File(getClass().getResource("/GeoLite2-City.mmdb").getFile()))
    
  // I am mapping the continents defined here with the enumerator
  // so in case in the future we need to change DB, we can just act here for the whole app
  def ipToContinent(ip: String) = {        
    val country = ipToCountry(ip).getIsoCode().toLowerCase()
    val continent = CountryContinent.getOrElse(country, unknown)  
    continent match {
      case unknown => ContinentsEnum.unknown
      case europe => ContinentsEnum.europe
      case northAmerica => ContinentsEnum.northAmerica
      case southAmerica => ContinentsEnum.southAmerica
      case asia => ContinentsEnum.asia
      case africa => ContinentsEnum.africa
      case oceania => ContinentsEnum.oceania
      case antarctica => ContinentsEnum.antarctica
    }
  }
  
  def ipToCountry(ip:String) = {
    ipToCity(ip).getCountry()
  }
  
  def ipToCity(ip: String) = {
    dbReader.city(InetAddress.getByName(ip))
  }
  

  val CountryContinent = collection.immutable.Map(
	"ad" -> europe,
	"ae" -> asia,
	"af" -> asia,
	"ag" -> northAmerica,
	"ai" -> northAmerica,
	"al" -> europe,
	"am" -> asia,
	"an" -> northAmerica,
	"ao" -> africa,
	"ap" -> asia,
	"aq" -> antarctica,
	"ar" -> southAmerica,
	"as" -> oceania,
	"at" -> europe,
	"au" -> oceania,
	"aw" -> northAmerica,
	"ax" -> europe,
	"az" -> asia,
	"ba" -> europe,
	"bb" -> northAmerica,
	"bd" -> asia,
	"be" -> europe,
	"bf" -> africa,
	"bg" -> europe,
	"bh" -> asia,
	"bi" -> africa,
	"bj" -> africa,
	"bl" -> northAmerica,
	"bm" -> northAmerica,
	"bn" -> asia,
	"bo" -> southAmerica,
	"br" -> southAmerica,
	"bs" -> northAmerica,
	"bt" -> asia,
	"bv" -> antarctica,
	"bw" -> africa,
	"by" -> europe,
	"bz" -> northAmerica,
	"ca" -> northAmerica,
	"cc" -> asia,
	"cd" -> africa,
	"cf" -> africa,
	"cg" -> africa,
	"ch" -> europe,
	"ci" -> africa,
	"ck" -> oceania,
	"cl" -> southAmerica,
	"cm" -> africa,
	"cn" -> asia,
	"co" -> southAmerica,
	"cr" -> northAmerica,
	"cu" -> northAmerica,
	"cv" -> africa,
	"cx" -> asia,
	"cy" -> asia,
	"cz" -> europe,
	"de" -> europe,
	"dj" -> africa,
	"dk" -> europe,
	"dm" -> northAmerica,
	"do" -> northAmerica,
	"dz" -> africa,
	"ec" -> southAmerica,
	"ee" -> europe,
	"eg" -> africa,
	"eh" -> africa,
	"er" -> africa,
	"es" -> europe,
	"et" -> africa,
	"eu" -> europe,
	"fi" -> europe,
	"fj" -> oceania,
	"fk" -> southAmerica,
	"fm" -> oceania,
	"fo" -> europe,
	"fr" -> europe,
	"fx" -> europe,
	"ga" -> africa,
	"gb" -> europe,
	"gd" -> northAmerica,
	"ge" -> asia,
	"gf" -> southAmerica,
	"gg" -> europe,
	"gh" -> africa,
	"gi" -> europe,
	"gl" -> northAmerica,
	"gm" -> africa,
	"gn" -> africa,
	"gp" -> northAmerica,
	"gq" -> africa,
	"gr" -> europe,
	"gs" -> antarctica,
	"gt" -> northAmerica,
	"gu" -> oceania,
	"gw" -> africa,
	"gy" -> southAmerica,
	"hk" -> asia,
	"hm" -> antarctica,
	"hn" -> northAmerica,
	"hr" -> europe,
	"ht" -> northAmerica,
	"hu" -> europe,
	"id" -> asia,
	"ie" -> europe,
	"il" -> asia,
	"im" -> europe,
	"in" -> asia,
	"io" -> asia,
	"iq" -> asia,
	"ir" -> asia,
	"is" -> europe,
	"it" -> europe,
	"je" -> europe,
	"jm" -> northAmerica,
	"jo" -> asia,
	"jp" -> asia,
	"ke" -> africa,
	"kg" -> asia,
	"kh" -> asia,
	"ki" -> oceania,
	"km" -> africa,
	"kn" -> northAmerica,
	"kp" -> asia,
	"kr" -> asia,
	"kw" -> asia,
	"ky" -> northAmerica,
	"kz" -> asia,
	"la" -> asia,
	"lb" -> asia,
	"lc" -> northAmerica,
	"li" -> europe,
	"lk" -> asia,
	"lr" -> africa,
	"ls" -> africa,
	"lt" -> europe,
	"lu" -> europe,
	"lv" -> europe,
	"ly" -> africa,
	"ma" -> africa,
	"mc" -> europe,
	"md" -> europe,
	"me" -> europe,
	"mf" -> northAmerica,
	"mg" -> africa,
	"mh" -> oceania,
	"mk" -> europe,
	"ml" -> africa,
	"mm" -> asia,
	"mn" -> asia,
	"mo" -> asia,
	"mp" -> oceania,
	"mq" -> northAmerica,
	"mr" -> africa,
	"ms" -> northAmerica,
	"mt" -> europe,
	"mu" -> africa,
	"mv" -> asia,
	"mw" -> africa,
	"mx" -> northAmerica,
	"my" -> asia,
	"mz" -> africa,
	"na" -> africa,
	"nc" -> oceania,
	"ne" -> africa,
	"nf" -> oceania,
	"ng" -> africa,
	"ni" -> northAmerica,
	"nl" -> europe,
	"no" -> europe,
	"np" -> asia,
	"nr" -> oceania,
	"nu" -> oceania,
	"nz" -> oceania,
	"om" -> asia,
	"pa" -> northAmerica,
	"pe" -> southAmerica,
	"pf" -> oceania,
	"pg" -> oceania,
	"ph" -> asia,
	"pk" -> asia,
	"pl" -> europe,
	"pm" -> northAmerica,
	"pn" -> oceania,
	"pr" -> northAmerica,
	"ps" -> asia,
	"pt" -> europe,
	"pw" -> oceania,
	"py" -> southAmerica,
	"qa" -> asia,
	"re" -> africa,
	"ro" -> europe,
	"rs" -> europe,
	"ru" -> europe,
	"rw" -> africa,
	"sa" -> asia,
	"sb" -> oceania,
	"sc" -> africa,
	"sd" -> africa,
	"se" -> europe,
	"sg" -> asia,
	"sh" -> africa,
	"si" -> europe,
	"sj" -> europe,
	"sk" -> europe,
	"sl" -> africa,
	"sm" -> europe,
	"sn" -> africa,
	"so" -> africa,
	"sr" -> southAmerica,
	"st" -> africa,
	"sv" -> northAmerica,
	"sy" -> asia,
	"sz" -> africa,
	"tc" -> northAmerica,
	"td" -> africa,
	"tf" -> antarctica,
	"tg" -> africa,
	"th" -> asia,
	"tj" -> asia,
	"tk" -> oceania,
	"tl" -> asia,
	"tm" -> asia,
	"tn" -> africa,
	"to" -> oceania,
	"tr" -> europe,
	"tt" -> northAmerica,
	"tv" -> oceania,
	"tw" -> asia,
	"tz" -> africa,
	"ua" -> europe,
	"ug" -> africa,
	"um" -> oceania,
	"us" -> northAmerica,
	"uy" -> southAmerica,
	"uz" -> asia,
	"va" -> europe,
	"vc" -> northAmerica,
	"ve" -> southAmerica,
	"vg" -> northAmerica,
	"vi" -> northAmerica,
	"vn" -> asia,
	"vu" -> oceania,
	"wf" -> oceania,
	"ws" -> oceania,
	"ye" -> asia,
	"yt" -> africa,
	"za" -> africa,
	"zm" -> africa,
	"zw" -> africa
)
  

}