package feral.functions.util

import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.Header
import org.http4s.Headers

import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders

import cats.effect.kernel.Concurrent
import cats.syntax.all._

import org.typelevel.ci.CIString



object Parser {
  def decodeRequest[F[_]: Concurrent](request: JSRequest): F[Request[F]] = 
    for {
      method <- Method.fromString(request.method).liftTo[F]
      uri <- Uri.fromString(request.url).liftTo[F]
      headers = {
        val builder = List.newBuilder[Header.Raw]
        val keys = JSHeaders.keyList(request.headers)

        keys.foreach(k => builder.addOne(Header.Raw(CIString(k), request.headers.get(k).getOrElse(""))))

        Headers(builder.result())
      }
    } yield Request[F](
      method = method,
      uri = uri,
      headers = headers
    )
}
