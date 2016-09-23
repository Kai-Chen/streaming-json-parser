package com.sorrentocorp.akka.stream

import akka.util.ByteString

sealed abstract class Neo4jRespToken(val str: ByteString) {
  override def toString = s"${getClass.getName}(${str.utf8String})"
}

case object EmptyResult extends Neo4jRespToken(ByteString.empty)

case class ResultColumn(meta: ByteString) extends Neo4jRespToken(meta)

case class DataRow(row: ByteString) extends Neo4jRespToken(row)

case object NoError extends Neo4jRespToken(ByteString.empty)
case class ErrorObj(errors: ByteString) extends Neo4jRespToken(errors)
