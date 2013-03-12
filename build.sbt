name := "SCALA-DNS"

version := "1.0"

scalaVersion := "2.10.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies += 	"io.netty" % "netty" % "3.6.0.Final"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.3"
 
libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0.3"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

libraryDependencies += "org.skife.com.typesafe.config" % "typesafe-config" % "0.3.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.1.3"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.1.3"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

// libraryDependencies += "org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.aws-java-sdk" % "1.3.27"
