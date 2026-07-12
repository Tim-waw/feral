package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._
import feral.functions.facade.InvocationContext

import org.http4s.HttpApp
//import cats.effect.IO

//import scala.scalajs.js.JSConverters._
// import org.http4s.nodejs.IncomingMessage
// import org.http4s.nodejs.ServerResponse

abstract class IOAzureHttpFunction {
  //val appFromIC: InvocationContext => HttpApp[IO]

  final def main(args: Array[String]): Unit =
    IOAzureHttpFunction.App.http(functionName, appConfig)

  protected val functionName: String = getClass.getSimpleName.init

  private val appConfig = js
    .Dynamic
    .literal(
      methods = js.Array("GET", "PUT"),
      authLevel = "anonymous",
      route = "{*path}",
      handler = handlerFn
    )

  private lazy val handlerFn
      : js.Function2[js.Any, InvocationContext, js.Promise[js.UndefOr[js.Any]]] = {
    (request, context) => {
      context.log("this is a log msg!!!")

      val response =
        js.Dynamic
          .literal(
            status = 200,
            body = "payload",
            headers = js.Dynamic.literal("content-type" -> "text/plain")
          )

      js.Promise.resolve[js.UndefOr[js.Any]](response)
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
