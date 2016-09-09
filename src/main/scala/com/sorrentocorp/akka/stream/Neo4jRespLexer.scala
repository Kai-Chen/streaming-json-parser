package com.sorrentocorp.akka.stream

import akka.util.ByteString

/** A simple state machine to tokenize json response from neo4j server */
class Neo4jRespLexer {
  import Neo4jRespLexer._

  private val Results = Expect("""{"results":""")
  private val EmptyArray = Expect("[]")
  private val Columns = Expect("""[{"columns":""")
  private val Data = Expect(""","data":[""")
  private val EndData = Expect("""]}]""")
  private val Errors = Expect(""","errors":""")

  private var state: State = INITIAL

  private var buffer: ByteString = ByteString.empty

  def offer(input: ByteString): Unit = buffer ++= input

  def poll: Option[Neo4jRespToken] =
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
        val (col, remainder) = array(buffer)
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
        val (row, remainder) = obj(buffer)
        buffer = remainder
        if (!row.isEmpty)
          Some(DataRow(row))
        else {
          state = END_ROWS
          poll
        }

      case END_ROWS =>
        EndData.offer(buffer)
        EndData.poll match {
          case None => None
          case Some(Left(x)) => badToken(EndData, x)
          case Some(Right(remainder)) =>
            buffer = remainder
            state = ERRORS
            poll
        }

      case ERRORS =>
        Errors.offer(buffer)
        Errors.poll match {
          case None => None
          case Some(Left(x)) => badToken(Errors, x)
          case Some(Right(remainder)) =>
            val (err, _) = array(remainder)
            buffer = ByteString.empty
            Some(ErrorObj(err))
        }
    }
}

object Neo4jRespLexer {
  final val OpenBracket = '['.toByte
  final val CloseBracket = ']'.toByte
  final val OpenBrace = '{'.toByte
  final val CloseBrace = '}'.toByte
  final val DoubleQuote = '"'.toByte
  final val Backslash = '\\'.toByte
  final val Comma = ','.toByte

  final val LineBreak = '\n'.toByte
  final val LineBreak2 = '\r'.toByte
  final val Tab = '\t'.toByte
  final val Space = ' '.toByte

  final val Whitespace = Set(LineBreak, LineBreak2, Tab, Space)

  def isWhitespace(input: Byte): Boolean =
    Whitespace.contains(input)

  /** Splits a ByteString at the first complete json array */
  def array(buf: ByteString): (ByteString, ByteString) =
    split(buf, OpenBracket, CloseBracket)

  def obj(buf: ByteString): (ByteString, ByteString) =
    split(buf, OpenBrace, CloseBrace)

  def split(buf: ByteString, open: Byte, close: Byte): (ByteString, ByteString) = {
    val (start, end) = matching(buf, open, close)
    if (start == -1)
      (ByteString.empty, buf)
    else
      buf.drop(start).splitAt(end - start)
  }

  /** Return a pair of indices, indicating the start and the end of a complete json array or object */
  def matching(buf: ByteString, open: Byte, close: Byte): (Int, Int) = {
    var depth = 0
    var inString = false
    var inEscape = false
    var found = false

    var start = -1
    var idx = -1
    while (buf.isDefinedAt(idx + 1) && !found) {
      idx += 1
      buf(idx) match {
        // no need to check for char sequence "\[" or "\{" as they are not legal in json
        case `open` if (!inString) =>
          start = idx
          depth += 1
        case `close` =>
          if (depth == 1 && !inString) found = true else depth -=1
        case DoubleQuote if (!inEscape) =>
          inString = !inString
        case Backslash =>
          inEscape = !inEscape
        case _ =>
          inEscape = false
      }
    }

    if (found) (start, idx+1) else (-1, 0)
  }

  sealed trait State
  case object INITIAL extends State
  case object RESULTS extends State
  case object COLUMNS extends State
  case object DATA extends State
  case object ROWS extends State
  case object END_ROWS extends State
  case object ERRORS extends State

  private def badToken(expect: Expect, input: ByteString) =
    throw new IllegalArgumentException(s"Expected [${expect.prefix.utf8String}] but found [${input.utf8String}]")
}
