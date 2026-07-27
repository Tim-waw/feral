package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._
import feral.functions.facade.InvocationContext

import org.http4s.HttpApp
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import cats.effect.unsafe.IORuntime

//import scala.scalajs.js.JSConverters._
import org.http4s.Request
import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders
import cats.effect.std.Dispatcher
import feral.functions.util.Parser
import fs2.text.utf8

abstract class IOAzureHttpFunction {
  protected def handler: InvocationContext => Resource[IO, HttpApp[IO]]

  private val runtime = IORuntime.global

  final def main(args: Array[String]): Unit =
    IOAzureHttpFunction.App.http(functionName, appConfig)

  private val functionName: String =
    getClass.getSimpleName.init // may want to add timestamp for testing

  private val appConfig = js
    .Dynamic
    .literal(
      methods = js.Array("GET", "PUT"),
      authLevel = "anonymous",
      route = "{*path}",
      handler = handlerFn
    )

  private lazy val handlerFn
      : js.Function2[JSRequest, InvocationContext, js.Promise[js.UndefOr[js.Any]]] = {
    val dispatcherHandle = {
      Dispatcher
        .parallel[IO](await = true)
        .product(Resource.pure(handler))
        .allocated
        .map(_._1) // drop unused finalizer, this resource will live for the duration
        .unsafeToPromise()(runtime)
    }

    (requestJS, context) => {
      // do dispatcher thing
      //context.log("befor FOR COMP")
      dispatcherHandle.`then`[js.Any] {
        case (dispatcher, handle) => {
          val io = for {
            _ <- IO(context.log("FOR COMP"))
            request <- Parser.decodeRequest[IO](requestJS)
            _ <- IO(context.log(request.uri.toString()))
            bodyList <- request.body.through(utf8.decode).compile.toList
            _ <- IO(context.log("req body: " + bodyList.toString()))
            
            response <- handle(context).use(app => app.run(request))
            _ <- IO(context.log(response.status.toString()))
            bodyList <- response.body.through(utf8.decode).compile.toList
            _ <- IO(context.log("resp body: " + bodyList.toString()))
            respEncoded <- Parser.encodeResponse[IO](response)
            _ <- IO(context.log("Decoded")) 
            _ <- IO(context.log(respEncoded.toString()))
          } yield respEncoded 
          
          dispatcher.unsafeToPromise(io)
        }
      }
      /////
      // impure!!!

      // val response =
      //   js.Dynamic
      //     .literal(
      //       status = 200,
      //       body = "payload",
      //       headers = js.Dynamic.literal("content-type" -> "text/plain")
      //     )

      // js.Promise.resolve[js.UndefOr[js.Any]](response)
    }
  }
}

object IOAzureHttpFunction {
  @js.native
  @JSImport("@azure/functions", "app")
  object App extends js.Object {
    def http(
        name: String,
        appConfig: js.Object
    ): Unit = js.native
  }
}
