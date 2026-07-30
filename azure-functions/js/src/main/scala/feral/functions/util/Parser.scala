package feral.functions.util

import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Response

import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders

//import cats.effect.kernel.Concurrent
import cats.effect.kernel.Async
import cats.syntax.all._
import cats.effect.syntax.all._

import org.typelevel.ci.CIString

import scala.scalajs.js
import feral.functions.facade.JSReadableStream
//import scala.scalajs.js.annotation._

import fs2.Stream
import cats.effect.std.Dispatcher
//import fs2.Chunk

import StreamUtil._
import cats.effect.std.Queue
import cats.effect.kernel.Fiber

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

  def encodeResponse[F[_]: Async](
      response: Response[F],
      dispatcher: Dispatcher[F]): F[js.Any] = {
    val headersList = response.headers.headers.map(h => (h.name.toString, h.value))
    val headers = js.Dictionary(headersList: _*)

    // val body = response.body //need to figure this out later

    val responseEncoded: js.Any = js
      .Dynamic
      .literal(
        status = response.status.code,
        headers = headers,
        body = toReadableStream[F](
          response.body,
          dispatcher
        ) // response.body.through(fs2.text.utf8.decode).compile.toString
      )

    responseEncoded.pure[F]
  }

  // probably want to change this to implement pull and cancel, this may work but is not the best impl
  private def toReadableStream[F[_]: Async](
      stream: Stream[F, Byte],
      dispatcher: Dispatcher[F]): js.Any = {
    js.Dynamic
      .newInstance(js.Dynamic.global.ReadableStream)(
        js.Dynamic
          .literal(
            start = (controller: js.Dynamic) => {
              val io = {
                stream
                  .chunks
                  .evalMap { chunk =>
                    Async[F].delay {
                      val array = new js.typedarray.Uint8Array(chunk.size)
                      chunk.toArray.zipWithIndex.foreach {
                        case (byte, index) => array(index) = byte
                      }
                      controller.enqueue(array)
                    }.void
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

  def encodeResponseV2[F[_]: Async](
      response: Response[F],
      dispatcher: Dispatcher[F],
      qBound: Int): F[js.Any] = {
    for {
      q <- Queue.bounded[F, StreamData](qBound)
      streamFiber <- createStreamFiber[F](response.body, q)
      headers <- createHeaders[F](response.headers)
      body <- createBodyStream[F](q, streamFiber, dispatcher)
    } yield {
      js.Dynamic
        .literal(
          status = response.status.code,
          headers = headers,
          body = body
        )
    }
  }

  def createStreamFiber[F[_]: Async](
      stream: Stream[F, Byte],
      q: Queue[F, StreamData]): F[Fiber[F, Throwable, Unit]] = {
    stream
      .chunks
      .evalMap(chunk => q.offer(Data(chunk)))
      .compile
      .drain
      .attempt
      .flatMap {
        case Right(_) => q.offer(End)
        case Left(e) => q.offer(Error(e))
      }
      .start
  }

  def createHeaders[F[_]: Async](responseHeaders: Headers): F[js.Dictionary[String]] = {
    val headersList = responseHeaders.headers.map(h => (h.name.toString, h.value))
    js.Dictionary(headersList: _*).pure[F]
  }

  def createBodyStream[F[_]: Async](
      q: Queue[F, StreamData],
      streamFiber: Fiber[F, Throwable, Unit],
      dispatcher: Dispatcher[F]): F[js.Any] = {
    val readableStream: js.Any = js
      .Dynamic
      .newInstance(js.Dynamic.global.ReadableStream)(
        js.Dynamic
          .literal(
            start = (controller: js.Dynamic) => { /*NoOp*/ },
            pull = (controller: js.Dynamic) => {
              val effect = createPullEffect[F](controller, q)
              dispatcher.unsafeToPromise(effect)
            },
            cancel = (reason: js.UndefOr[js.Any]) => {
              dispatcher.unsafeToPromise(streamFiber.cancel)
            }
          )
      )

    readableStream.pure[F]
  }

  def createPullEffect[F[_]: Async](
      controller: js.Dynamic,
      q: Queue[F, StreamData]): F[Unit] = {
    q.take.flatMap {
      case Error(e) => {
        Async[F].delay(controller.error(e.getMessage)).void
      }
      case End => {
        Async[F].delay(controller.close()).void
      }
      case Data(chunk) => {
        Async[F].delay {
          val array = new js.typedarray.Uint8Array(chunk.size)
          chunk.toArray.zipWithIndex.foreach {
            case (byte, index) => array(index) = (byte & 0xff).toShort
          }
          controller.enqueue(array)
        }.void
      }
    }
  }
}
