package com.sorrentocorp.akka.stream

import akka.actor._
import akka.stream._, scaladsl._
import akka.util.ByteString
import org.scalatest._
import scala.concurrent._, duration._

class Neo4jRespFramingSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("test-Neo4jRespFramingSpec")
  implicit val materializer = ActorMaterializer()

  override def afterAll = {
    materializer.shutdown()
    system.terminate()
  }

  "Neo4jRespFraming" should "emit complete json objects" in {
    val src = Source.single(ByteString("""{"results":[{"columns":["n"],"data":[{"row":[{"arr":[1,2,3],"i":1,"anormcyphername":"nprops","arrc":["a","b","c"]}]}]}],"errors":[]}""")).via(new Neo4jRespFraming)

    val future = src.runWith(Sink.seq)
    val res = Await.result(future, 1.second)
    res shouldBe Seq(ResultColumn(ByteString("""["n"]""")),
      DataRow(ByteString("""{"row":[{"arr":[1,2,3],"i":1,"anormcyphername":"nprops","arrc":["a","b","c"]}]}""")),
      NoError)
  }
}
