package feral.functions.util

import fs2.Chunk

object StreamUtil {
  sealed trait StreamData
  final case class Data(chunk: Chunk[Byte]) extends StreamData
  case object End extends StreamData
  final case class Error(e: Throwable) extends StreamData
}
