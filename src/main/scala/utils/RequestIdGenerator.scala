package utils

/*
 * TODO: Functional way required
 */

object RequestIdGenerator {
  private var currentId = 1
  
  def generateId = {
    val id = currentId
    currentId += 1
    id
  }
}