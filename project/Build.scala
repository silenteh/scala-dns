import sbt._
import Keys._

object ScalaDnsBuild extends Build {
  
  System.setProperty("config.file", "conf/application.conf")
  
  lazy val root = Project(id = "scala-dns", base = file("."), settings = Project.defaultSettings)
  
}