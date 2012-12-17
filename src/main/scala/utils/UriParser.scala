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
package utils

import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import collection.JavaConversions._
import org.jboss.netty.handler.codec.http.multipart.Attribute
import java.net.URLDecoder

object UriParser {
  
  def uriPath(request: HttpRequest): List[String] = uriPath(request.getUri)
  
  def uriPath(uri: String): List[String] = uriPathString(URLDecoder.decode(uri, "UTF-8")).split("/").filterNot(_.trim.isEmpty).toList
  
  def uriPathString(request: HttpRequest): String = uriPathString(request.getUri)
  
  def uriPathString(uri: String): String = {
    val decUri = URLDecoder.decode(uri, "UTF-8")
    val cleanUri = if (decUri.contains("?")) decUri.trim.substring(0, decUri.trim.indexOf("?")) else decUri.trim
    if (cleanUri.endsWith("/")) cleanUri.substring(0, cleanUri.length - 1) else cleanUri
  }
  
  def uriQueryString(request: HttpRequest): Map[String, String] = uriQueryString(request.getUri)
  
  def uriQueryString(uri: String): Map[String, String] = 
    uriRawQueryString(URLDecoder.decode(uri, "UTF-8")).split("&").toList.collect{p =>
      p.split("=").toList match {
        case List(k, v) => (k, v)
        case List(k) => (k, "")
      }
    }.toMap
  
  def uriRawQueryString(request: HttpRequest): String = uriRawQueryString(request.getUri)
  
  def uriRawQueryString(uri: String): String = if(!uri.contains("?")) "" else {
    val decUri = URLDecoder.decode(uri, "UTF-8")
    val qStrStart = decUri.indexOf("?")
    
    decUri.substring(qStrStart + 1, decUri.length - 1)
  }
  
  def postParams(request: HttpRequest) = if(request.getMethod != HttpMethod.POST) null else {
    val decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory, request)
    decoder.getBodyHttpDatas().map(_ match { case data: Attribute => (data.getName, data.getValue) }).toMap
  }
  
}