/**
 * Copyright (C) 2015-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.scaladsl

import akka.NotUsed
import akka.stream.Attributes
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.stage.{ InHandler, OutHandler, GraphStageLogic }
import akka.util.ByteString

import com.sorrentocorp.akka.stream.impl.JsonObjectParser

import scala.util.control.NonFatal

/** Provides JSON framing stages that can separate valid JSON objects from incoming [[ByteString]] objects. */
object JsonFraming {

  /**
   * Returns a Flow that implements a "brace counting" based framing stage for emitting valid JSON chunks.
   * It scans the incoming data stream for valid JSON objects and returns chunks of ByteStrings containing only those valid chunks.
   *
   * Typical examples of data that one may want to frame using this stage include:
   *
   * **Very large arrays**:
   * {{{
   *   [{"id": 1}, {"id": 2}, [...], {"id": 999}]
   * }}}
   *
   * **Multiple concatenated JSON objects** (with, or without commas between them):
   *
   * {{{
   *   {"id": 1}, {"id": 2}, [...], {"id": 999}
   * }}}
   *
   * The framing works independently of formatting, i.e. it will still emit valid JSON elements even if two
   * elements are separated by multiple newlines or other whitespace characters. And of course is insensitive
   * (and does not impact the emitting frame) to the JSON object's internal formatting.
   *
   * @param maximumObjectLength The maximum length of allowed frames while decoding. If the maximum length is exceeded
   *                            this Flow will fail the stream.
   */
  sealed trait State
  case object INITIAL extends State
  case object MATCHED_OPEN_BRACE extends State
  case object MATCHED_RESULT extends State

  def objectScanner(maximumObjectLength: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new SimpleLinearGraphStage[ByteString] {

      override protected def initialAttributes: Attributes = Attributes.name("JsonFraming.objectScanner")

      override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) with InHandler with OutHandler {
        private var state: State = INITIAL
        private var tmp: ByteString = ByteString.empty

        private val buffer = new JsonObjectParser(maximumObjectLength)

        setHandlers(in, out, this)

        override def onPush(): Unit = {
          println("pushed")
          val curr = grab(in)

          state match {
            case INITIAL => tmp ++= curr
            case _ => buffer.offer(curr)
          }

          tryPopBuffer()
        }

        override def onPull(): Unit = {
          println("pulled")
          tryPopBuffer()
        }

        override def onUpstreamFinish(): Unit = {
          println("upstream completed")
          buffer.poll() match {
            case Some(json) ⇒ emit(out, json)
            case _          ⇒ completeStage()
          }
        }

        def tryPopBuffer(): Unit = {
          println(s"state is $state")
          state match {
            case INITIAL =>
              val buf = tmp.dropWhile(JsonObjectParser.isWhitespace(_))
              if (!buf.isEmpty) {
                buf(0) match {
                  case '{' =>
                    println("matched '{'")
                    tmp = ByteString.empty
                    state = MATCHED_OPEN_BRACE
                    buffer.offer(buf.drop(11))
                    tryPopBuffer()
                  case _ =>
                    failStage(new IllegalArgumentException(s"expected '{' but found ${buf(0)}"))
                }
              } else {
                pull(in)
              }
            case _ =>
              try buffer.poll() match {
                case Some(json) ⇒ push(out, json)
                case _          ⇒ if (isClosed(in)) completeStage() else pull(in)
              } catch {
                case NonFatal(ex) ⇒ failStage(ex)
              }
          }
        }
      }
    })

}
