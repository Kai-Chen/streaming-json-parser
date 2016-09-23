package com.sorrentocorp.akka.stream

import akka.actor._
import akka.stream._, scaladsl._
import akka.util.ByteString
import org.scalatest._

class Neo4jRespFramingSpec extends FlatSpec with BeforeAndAfterAll {
  implicit val system = ActorSystem("test-Neo4jRespFramingSpec")
  implicit val materializer = ActorMaterializer()

  override def afterAll = {
    materializer.shutdown()
    system.terminate()
  }

  "Neo4jRespFraming" should "emit complete json objects" in {
    Source.single(ByteString("""{"results":[{"columns":["n"],"data":[{"row":[{"arr":[1,2,3],"i":1,"anormcyphername":"nprops","arrc":["a","b","c"]}]}]}],"errors":[]}""")).via(new Neo4jRespFraming).runForeach(println)
  }
}
