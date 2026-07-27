package feral.functions.facade

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

import fs2.Stream
import fs2.Chunk

//import cats.effect.IO
import cats.effect.kernel.Async
import cats.syntax.all._

//may want to change to requestFacade.., think it over

@js.native
trait JSRequest extends js.Object {
  def method: String = js.native
  def url: String = js.native
  def headers: JSHeaders = js.native
  def body: js.UndefOr[JSReadableStream] = js.native
}

@js.native
trait JSHeaders extends js.Object {
  def get(name: String): js.UndefOr[String] = js.native
  def keys(): js.Iterator[String] = js.native 
}

@js.native
trait JSReadableStream extends js.Object {
  def getReader(): JSReadableStreamDefaultReader = js.native
}

@js.native
trait JSReadableStreamDefaultReader extends js.Object {
  def read(): js.Promise[JSReadObject] = js.native
  def releaseLock(): Unit = js.native
  def cancel(reason: js.UndefOr[js.Any]): js.Promise[Unit]
}

@js.native
trait JSReadObject extends js.Object {
  def value: js.UndefOr[Uint8Array] = js.native
  def done: Boolean = js.native
}

object JSHeaders {
  def keyList(h: JSHeaders): List[String] = {
    val builder = List.newBuilder[String]
    val itr = h.keys()
    var entity = itr.next()

    while(!entity.done) {
      builder.addOne(entity.value)
      entity = itr.next()
    }

    builder.result()
  }

  object Syntax {
    //syntax for method like calls???
  }
}

object JSReadableStream {
  def toFs2[F[_]: Async](streamOption: js.UndefOr[JSReadableStream]): Stream[F, Byte] = {
    streamOption.toOption match {
      case None => Stream.empty
      case Some(null) => Stream.empty
      case Some(stream) => {
        Stream.eval(Async[F].delay(stream.getReader())).flatMap{ reader => 
          def nextChunk = {
            Async[F].fromPromise(Async[F].delay(reader.read())).map{ read => 
              if(read.done) {
                None
              } else {
                val chunk = read.value
                  .toOption
                  .map(arr => Chunk.array[Byte](toByteArray(arr)))
                  .getOrElse(Chunk.empty[Byte])
                  
                Some(chunk)
              }
            }
          }

          Stream
            .repeatEval(nextChunk)
            .unNoneTerminate
            .flatMap(Stream.chunk)
            .onFinalize(Async[F].delay(reader.releaseLock()))
        }
      }
    }
  }

  private def toByteArray(array: Uint8Array): Array[Byte] = { 
    val builder = Array.newBuilder[Byte]
    val length = array.length
    var index = 0

    while(index != length) {
      builder.addOne(array(index).toByte)
      index = index + 1
    }

    builder.result()
  }

  object Syntax {}
}

/* Parameters

method: Method.GET, Method.POST, etc.
uri: representation of the request URI
httpVersion: the HTTP version //not used in lambda-http4s
headers: collection of Headers
body: fs2.Stream[F, Byte] defining the body of the request
attributes: Immutable Map used for carrying additional information in a type safe fashion //not used in lambda-http4s
*/