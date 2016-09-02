name := "streaming-json-parser"
organization := "com.sorrentocorp"
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature", "-deprecation")
val akkaVersion = "2.4.8"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)
