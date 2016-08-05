package example

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

object Graph {
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val out = Sink.foreach(println)
    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge
    ClosedShape
  })

  val topHeadSink = Sink.head[Int]
  val bottomHeadSink = Sink.head[Int]
  val sharedDoubler = Flow[Int].map(_ * 2)
  val g2 = RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) { implicit builder =>
    (topHS, bottomHS) =>
    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[Int](2))
    Source.single(1) ~> broadcast.in
    broadcast.out(0) ~> sharedDoubler ~> topHS.in
    broadcast.out(1) ~> sharedDoubler ~> bottomHS.in
    ClosedShape
  })

}
