package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._
import feral.functions.facade.InvocationContext

import org.http4s.HttpApp
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import cats.effect.unsafe.IORuntime


import feral.functions.facade.JSRequest
import cats.effect.std.Dispatcher
import feral.functions.util.Parser
import feral.functions.util.AppConfig.buildDefaultConfig

abstract class IOAzureHttpFunction {
  protected def handler: InvocationContext => Resource[IO, HttpApp[IO]]
  protected def appConfig = buildDefaultConfig(handlerFn).toJS
  protected def qBound: Int = 100

  private val runtime = IORuntime.global

  final def main(args: Array[String]): Unit =
    IOAzureHttpFunction.App.http(functionName, appConfig)

  private val functionName: String =
    getClass.getSimpleName.init

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
      dispatcherHandle.`then`[js.Any] {
        case (dispatcher, handle) => {
          val io = for {
            request <- Parser.decodeRequest[IO](requestJS)
            response <- handle(context).use(app => app.run(request))
            respEncoded <- Parser.encodeResponse[IO](response, dispatcher, qBound)
          } yield respEncoded

          dispatcher.unsafeToPromise(io)
        }
      }
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
