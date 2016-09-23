name := "streaming-json-parser"
organization := "com.sorrentocorp"
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature", "-deprecation")
val akkaVersion = "2.4.8"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test"
)
