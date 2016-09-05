package com.sorrentocorp.akka.stream

import akka.NotUsed
import akka.stream.Attributes
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.scaladsl._
import akka.stream.stage.{ InHandler, OutHandler, GraphStageLogic }
import akka.util.ByteString

import scala.util._, control.NonFatal

object Neo4jRespFraming {
  def scanner: Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new SimpleLinearGraphStage[ByteString] {

      override protected def initialAttributes: Attributes = Attributes.name("Neo4jRespFraming.scanner")

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
            case Some(token) => emit(out, token.str)
            case None => completeStage()
          }
        }

        def tryPopBuffer(): Unit = {
          try lexer.poll match {
            case Some(token) => emit(out, token.str)
            case None => if (isClosed(in)) completeStage() else pull(in)
          } catch {
            case NonFatal(ex) => failStage(ex)
          }
        }
      }
    })

}
