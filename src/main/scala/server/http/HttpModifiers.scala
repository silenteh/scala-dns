package server.http

import tools.PipelineModifier
import server.http.file.FileHttpModifier
import server.http.json.JsonHttpModifier

object HttpModifiers {
  def apply() = Map[String, PipelineModifier] (
    FileHttpModifier.name -> new FileHttpModifier,
    JsonHttpModifier.name -> new JsonHttpModifier
  )
}