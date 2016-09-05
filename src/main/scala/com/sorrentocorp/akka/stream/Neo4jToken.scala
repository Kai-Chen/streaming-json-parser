package com.sorrentocorp.akka.stream

import akka.util.ByteString

sealed abstract class Neo4jToken(val str: ByteString) {
  override def toString = s"${getClass.getName}(${str.utf8String})"
}

case object EmptyResult extends Neo4jToken(ByteString.empty)

case class ResultColumn(meta: ByteString) extends Neo4jToken(meta)

case class DataRow(row: ByteString) extends Neo4jToken(row)

case class ErrorObj(errors: ByteString) extends Neo4jToken(errors)
