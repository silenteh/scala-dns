package utils

import models.User
import datastructures.UserCache
import domainio.JsonIO

object UserCreator {
	def apply(username: String, password: String) = {
	  val user = new User(username, Sha256Digest(password))
	  
	  if(UserCache.users.exists(_.name == user.name)) 
	    "User already exists"
	  else {
	    UserCache.addUser(user)
	    JsonIO.updateUsers()
	    "User was added successfully"
	  }
	}
}