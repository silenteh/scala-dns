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
package datastructures

import scala.collection.mutable
import models.User
import java.security.MessageDigest
import utils.Sha256Digest

object UserCache {
	val users = mutable.Set[User]()
	
	def addUser(username: String, password: String) = users += new User(username, Sha256Digest(password))
	
	def addUser(user: User) = users += user
	
	def removeUser(user: User): Unit = if(users.contains(user)) users -= user
	
	def removeUser(name: String): Unit = 
	  users.find(u => u.name == name) match {
	    case Some(user) => removeUser(user)
	    case None => Unit
	  }
	
	def findUser(name: String) = 
	  users.find(u => u.name == name) match {
	    case Some(user) => user
	    case None => null
	  }
	
	def findUser(name: String, password: String) = 
	  users.find(u => u.name == name && u.passwordDigest == Sha256Digest(password)) match {
	    case Some(user) => user
	    case None => null
	  }
}