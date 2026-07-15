package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._
import feral.functions.facade.InvocationContext

import org.http4s.HttpApp
import cats.effect.IO
import cats.effect.Resource

//import scala.scalajs.js.JSConverters._
import org.http4s.Request
import feral.functions.facade.JSRequest
import feral.functions.facade.JSHeaders

abstract class IOAzureHttpFunction {
  def buildHttpApp(context: InvocationContext): Resource[IO, HttpApp[IO]]

  final def main(args: Array[String]): Unit =
    IOAzureHttpFunction.App.http(functionName, appConfig)

  protected val functionName: String = getClass.getSimpleName.init //may want to add timestamp for testing

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
    (request, context) => {
      val h = request.headers
      val headers = JSHeaders.keyList(h)
      context.log(s"headers: $headers")

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
