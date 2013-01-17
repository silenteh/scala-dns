package models

import com.fasterxml.jackson.annotation.JsonProperty

case class User(
  @JsonProperty("name") name: String, 
  @JsonProperty("digest") passwordDigest: String
)