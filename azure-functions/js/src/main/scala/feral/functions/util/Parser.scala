package feral.functions.util

import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Response

import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders

import cats.effect.kernel.Concurrent
import cats.effect.kernel.Async
import cats.syntax.all._

import org.typelevel.ci.CIString

import scala.scalajs.js
import feral.functions.facade.JSReadableStream
//import scala.scalajs.js.annotation._

import fs2.Stream
import cats.effect.std.Dispatcher

object Parser {
  def decodeRequest[F[_]: Async](request: JSRequest): F[Request[F]] = {
    for {
      method <- Method.fromString(request.method).liftTo[F]
      uri <- Uri.fromString(request.url).liftTo[F]
      body = JSReadableStream.toFs2[F](request.body)
      headers = {
        val builder = List.newBuilder[Header.Raw]
        val keys = JSHeaders.keyList(request.headers)

        keys.foreach(k =>
          builder.addOne(Header.Raw(CIString(k), request.headers.get(k).getOrElse(""))))

        Headers(builder.result())
      }
      // body = JSReadableStream.toFs2[F](request.body) //need to use generic type parameter in tofs2
    } yield Request[F](
      method = method,
      uri = uri,
      headers = headers,
      body = body // need to convert into Entity
    )
  }

  def encodeResponse[F[_]: Async](response: Response[F], dispatcher: Dispatcher[F]): F[js.Any] = {
    val headersList = response.headers.headers.map(h => (h.name.toString, h.value))
    val headers = js.Dictionary(headersList: _*)

    // val body = response.body //need to figure this out later

    val responseEncoded: js.Any = js
      .Dynamic
      .literal(
        status = response.status.code,
        headers = headers,
        body = toReadableStream[F](response.body, dispatcher)//response.body.through(fs2.text.utf8.decode).compile.toString
      )

    // val resp: js.Any =
    //     js.Dynamic
    //       .literal(
    //         status = 200,
    //         body = "payload",
    //         headers = js.Dynamic.literal("content-type" -> "text/plain")
    //       )

    responseEncoded.pure[F]
    // resp.pure[F]
  }

  private def toReadableStream[F[_]: Async](
      stream: Stream[F, Byte],
      dispatcher: Dispatcher[F]): js.Any = {
    js.Dynamic.newInstance(js.Dynamic.global.ReadableStream)(
      js.Dynamic.literal(
        start = (controller: js.Dynamic) => {
          val io = {
            stream.chunks.evalMap { chunk =>
              Async[F].delay {
                val array = new js.typedarray.Uint8Array(chunk.size) 
                chunk.toArray.zipWithIndex.foreach{ case (byte, index) => array(index) = byte }
                controller.enqueue(array)
              }
              .void
            }
            .onFinalize(Async[F].delay(controller.close()).void)
            .compile
            .drain
          }
          dispatcher.unsafeToPromise(io)
        }
      )
    )
  }
}
