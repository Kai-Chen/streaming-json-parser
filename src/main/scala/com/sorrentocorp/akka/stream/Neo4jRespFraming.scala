package com.sorrentocorp.akka.stream

import akka.NotUsed
import akka.stream._, scaladsl._, stage._
import akka.util.ByteString

import scala.util._, control.NonFatal

class Neo4jRespFraming extends GraphStage[FlowShape[ByteString, Neo4jRespToken]] {
  override protected def initialAttributes: Attributes = Attributes.name("Neo4jRespFraming.scanner")

  val in = Inlet[ByteString]("Neo4jRespFraming.in")
  val out = Outlet[Neo4jRespToken]("Neo4jRespFraming.out")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with InHandler with OutHandler {
    private val lexer = new Neo4jRespLexer

    setHandlers(in, out, this)

    override def onPush(): Unit = {
      val curr = grab(in)
      lexer.offer(curr)
      tryPopBuffer()
    }

    override def onPull(): Unit = {
      tryPopBuffer()
    }

    override def onUpstreamFinish(): Unit = {
      lexer.poll match {
        case Some(token) => emit(out, token)
        case None => completeStage()
      }
    }

    def tryPopBuffer(): Unit = {
      try lexer.poll match {
        case Some(token) => emit(out, token)
        case None => if (isClosed(in)) completeStage() else pull(in)
      } catch {
        case NonFatal(ex) => failStage(ex)
      }
    }
  }
}
