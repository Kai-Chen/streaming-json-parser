name := "streaming-json-parser"
organization := "com.sorrentocorp"
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature", "-deprecation")
val akkaVersion = "2.4.8"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

initialCommands in console := """
import akka.actor._
import akka.stream._, scaladsl._
implicit val system = ActorSystem("example")
implicit val mat = ActorMaterializer()
"""

cleanupCommands in console := """
mat.shutdown()
system.terminate()
"""
