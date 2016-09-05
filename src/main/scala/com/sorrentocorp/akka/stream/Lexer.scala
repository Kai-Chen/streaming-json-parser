package com.sorrentocorp.akka.stream

import akka.util.ByteString

class Lexer {
  sealed trait State
  case object INITIAL extends State
  case object RESULTS extends State
  case object EMPTY extends State
  case object COLUMNS extends State
  case object DATA extends State
  case object ERRORS extends State

  private val Results = Expect("""{"results":""")
  private val EmptyArray = Expect("[]")
  private val Columns = Expect("""[{"columns":""")
  private val Data = Expect(""","data":[""")
  private val Errors = Expect(""","errors":""")

  private var state: State = INITIAL

  private var buffer: ByteString = ByteString.empty

  def offer(input: ByteString): Unit = buffer ++= input

  def poll: Option[Neo4jToken] =
    if (buffer.isEmpty) None else state match {
      case INITIAL =>
        Results.offer(buffer)
        Results.poll match {
          case None =>
            None
          case Some(Left(x)) =>
            badToken(Results, x)
          case Some(Right(remainder)) =>
            buffer = remainder
            state = RESULTS
            poll
        }
      // case RESULTS =>
      //   EmptyArray.offer(buffer)
      //   EmptyArray.poll match {
      //     case None => None
      //     case Some(Right(remainder)) =>
      //   }
      // case DATA =>
      // case ERRORS =>
      case _ =>
        val ret = Some(ResultColumn(buffer))
        buffer = ByteString.empty
        ret
    }

  private def badToken(expect: Expect, input: ByteString) =
    throw new IllegalArgumentException(s"Expected [${expect.prefix.utf8String}] but found [${input.utf8String}]")
}
