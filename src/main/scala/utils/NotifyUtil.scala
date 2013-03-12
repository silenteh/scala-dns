package utils

import enums.RecordType
import configs.ConfigService
import client.DNSClient
import models.ExtendedDomain
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory

/**
 * *****************************************************************************
 * Copyright 2013 silenteh
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
 * ****************************************************************************
 */
 
 object NotifyUtil {

	val logger = LoggerFactory.getLogger("DNS Notify")
	
	
  def notify(domain :ExtendedDomain) {

    val notifyQuestion = List((domain.fullName.split("""\.""").toList,RecordType.SOA.id,1))
    ConfigService.config.getStringList("zoneTransferAllowedIps").toList.foreach { e =>
      logger.debug("Notification is about to be sent")
      DNSClient.sendNotify(e,53,notifyQuestion){future =>
      logger.debug("Notify message sent")
    }
    }

  }

}
