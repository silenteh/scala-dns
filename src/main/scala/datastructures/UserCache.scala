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