package domainio

import org.slf4j.LoggerFactory
import models.User
import datastructures.UserCache

object UserValidationService {

  val logger = LoggerFactory.getLogger("app")
  
  def validate(user: User, oldName: String = null) = 
    if(user == null) (false, validationMessages("unknownerror") :: Nil)
    else {
      val name = validateUserName(user.name, oldName)
      val password = validatePassword(user.passwordDigest)
      (name._1 && password._1, (name._2 :: password._2 :: Nil).filterNot(_ == null))
    }
  
  def validateUserName(name: String, oldName: String = null) = 
    if(name.length < 1 || name.length > 255) (false, validationMessages("required").format("User name"))
    else {
      val exclude = if(oldName == null) Nil else oldName :: Nil
      isUnique(name, UserCache.users.map(_.name).toList, exclude)
    }
  
  def validatePassword(password: String) = 
    if(password.length < 1) (false, validationMessages("required").format("Password"))
    else (true, null)
  
  def isUnique(item: String, items: List[String], exclude: List[String] = Nil) = 
    if (exclude.contains(item) || items.filter(_ == item).isEmpty) (true, null)
    else (false, validationMessages("duplicate").format(item))
  
  val validationMessages = Map(
    "invalid" -> "%s is invalid",
    "required" -> "%s is required",
    "duplicate" -> "%s already exists",
    "unknownerror" -> "Unknown error"
  )
  
}