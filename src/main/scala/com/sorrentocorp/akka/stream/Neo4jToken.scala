package com.sorrentocorp.akka.stream

import akka.util.ByteString

sealed abstract class Neo4jToken(val str: ByteString) {
  override def toString = s"${getClass.getName}($str.utf8String)"
}

case class ResultColumn(meta: ByteString) extends Neo4jToken(meta)

case class DataRow(row: ByteString) extends Neo4jToken(row) {
  override def toString = s"DataRow(${row.utf8String})"
}

case class Errors(errors: ByteString) extends Neo4jToken(errors) {
  override def toString = s"Errors(${errors.utf8String})"
}
