package configs
import com.typesafe.config.ConfigFactory

object ConfigService {
  
  lazy val config = ConfigFactory.load()

}