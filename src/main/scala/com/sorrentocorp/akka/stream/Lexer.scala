package com.sorrentocorp.akka.stream

import akka.util.ByteString
import impl.JsonObjectParser

/** A simple state machine to tokenize json response from neo4j server */
class Lexer {
  sealed trait State
  case object INITIAL extends State
  case object RESULTS extends State
  case object COLUMNS extends State
  case object DATA extends State
  case object ROWS extends State
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
          case None => None
          case Some(Left(x)) => badToken(Results, x)
          case Some(Right(remainder)) =>
            buffer = remainder
            state = RESULTS
            poll
        }

      case RESULTS =>
        // try match empty array first
        EmptyArray.offer(buffer)
        EmptyArray.poll match {
          case None => None
          case Some(Right(remainder)) =>
            buffer = remainder
            state = ERRORS
            Some(EmptyResult)
          case Some(Left(remainder)) =>
            // empty match fails, so we know there is some data in the
            // results; next try match columns meta data
            Columns.offer(buffer)
            Columns.poll match {
              case None => None
              case Some(Left(x)) => badToken(Columns, x)
              case Some(Right(remainder)) =>
                buffer = remainder
                state = COLUMNS
                poll
            }
        }

      case COLUMNS =>
        val (col, remainder) = JsonObjectParser.array(buffer)
        buffer = remainder
        state = DATA
        Some(ResultColumn(col))

      case DATA =>
        Data.offer(buffer)
        Data.poll match {
          case None => None
          case Some(Left(x)) => badToken(Data, x)
          case Some(Right(remainder)) =>
            buffer = remainder
            state = ROWS
            poll
        }

      case ROWS =>
        val (row, remainder) = JsonObjectParser.obj(buffer)
        buffer = remainder
        if (!row.isEmpty)
          Some(DataRow(row))
        else {
          state = ERRORS
          poll
        }

      case ERRORS =>
        val ret = Some(ErrorObj(buffer))
        buffer = ByteString.empty
        ret
    }

  private def badToken(expect: Expect, input: ByteString) =
    throw new IllegalArgumentException(s"Expected [${expect.prefix.utf8String}] but found [${input.utf8String}]")
}
