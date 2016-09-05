package com.sorrentocorp.akka.stream

import akka.util.ByteString
import scala.util._

/** Tries to match a ByteString literal. */
class Expect(val prefix: ByteString) {
  require(!prefix.isEmpty)
  val prefixLen = prefix.size
  private var buffer: ByteString = ByteString.empty

  def offer(input: ByteString): Unit = buffer ++= input

  /** Before prefix is matched, returns None; after matching, returns rest of the input */
  def poll: Option[Either[ByteString, ByteString]] =
    if (buffer.size < prefixLen)
      None
    else if (buffer.take(prefixLen) == prefix)
      Some(Right(buffer.drop(prefixLen)))
    else
      Some(Left(buffer))
}

object Expect {
  def apply(str: String) = new Expect(ByteString(str))
}
