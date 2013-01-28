package client

import payload.Question
import collection.mutable
import payload.Message
import utils.RequestIdGenerator
import payload.Header
import enums.ResponseCode
import payload.Message
import org.slf4j.LoggerFactory
import collection.JavaConversions._
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import enums.RecordType

object DNSClient {

  val logger = LoggerFactory.getLogger("app")
  val addresses: mutable.ConcurrentMap[Int, (String, Int)] = new ConcurrentHashMap[Int, (String, Int)]
  val callbacks: mutable.ConcurrentMap[Int, Message => Unit] = new ConcurrentHashMap[Int, Message => Unit]
  
  def send(address: String, port: Int, questions: List[(List[String], Int, Int)])(callback: Message => Unit): Unit = {
    val questionArray = questions.map { case(qname, qtype, qclass) =>
      Question(qname.map(_.getBytes) ++ (Array[Byte]() :: Nil), qtype, qclass)
    }.toArray
    send(address, port, questionArray)(callback)
  }
  
  def send(address: String, port: Int, questions: Array[Question])(callback: Message => Unit): Unit = {
    val id = RequestIdGenerator.generateId
    val header = Header(id, false, 0, false, false, true, false, 0, ResponseCode.OK.id, questions.length, 0, 0, 0)
    val request = Message(header, questions, Array(), Array(), Array())
    addCallback(id, address, port, callback)
    if(questions.exists(_.qtype == RecordType.AXFR.id)) {
      sendTCP(address, port, request)
    } else {
      sendUDP(address, port, request)
    }
  }
  
  def processResponse(response: Message): Unit = 
    if(response.header.truncated) {
      logger.debug("Response truncated, switching to TCP ...")
      getAddress(response.header.id) { case(address, port) =>
        val id = RequestIdGenerator.generateId
        val header = Header(id, false, 0, false, false, true, false, 0, ResponseCode.OK.id, response.query.length, 0, 0, 0)
        val request = Message(header, response.query, Array(), Array(), Array())
          
        getCallback(response.header.id) {callback =>
          removeCallback(response.header.id)
          addCallback(id, address, port, callback)
        }
          
        sendTCP(address, port, request)
      }
    } else {
      getCallback(response.header.id) {_.apply(response)}
      removeCallback(response.header.id)
    }
  
  private def sendUDP(address: String, port: Int, message: Message) = 
    UDPDnsClient.send(address, port, message) {future =>
      logger.debug("UDP message sent")
    }
  
  private def sendTCP(address: String, port: Int, message: Message) = {
    TCPDnsClient.send(address, port, message) {future =>
      logger.debug("TCP message sent")
    }
  }
  
  private def addCallback(id: Int, address: String, port: Int, callback: Message => Unit): Unit = {
    callbacks += (id -> callback)
    addresses += (id -> (address, port))
  }
  
  private def removeCallback(id: Int): Unit = {
    callbacks -= id
    addresses -= id
  }
  
  private def getCallback(id: Int)(fn: (Message => Unit) => Unit) = 
    callbacks.get(id) match {
      case Some(callback) => fn(callback)
      case None => Unit
    }
  
  private def getAddress(id: Int)(fn: (String, Int) => Unit) = 
    addresses.get(id) match {
      case Some((address, port)) => fn(address, port)
      case None => Unit
    }
}