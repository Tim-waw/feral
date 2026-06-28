package feral.functions

import scala.scalajs.js
import scala.scalajs.js.annotation._
//import scala.scalajs.js.JSConverters._

@js.native
@JSImport("@azure/functions", "app")
object App extends js.Object {
  def http(
      name: String,
      appConfig: js.Object
  ): Unit = js.native
}

abstract class IOAzureHttpFunction {
  final def main(args: Array[String]): Unit =
    App.http(functionName, appConfig)

  protected val functionName: String = getClass.getSimpleName.init

  private val appConfig = js.Dynamic.literal(
    methods = js.Array("GET", "PUT"),
    authLevel = "anonymous",
    route = "{*path}",
    handler = handlerFn
  )

  private lazy val handlerFn: js.Function2[js.Any, js.Any, js.Promise[js.UndefOr[js.Any]]] = {
    (request, context) => js.Promise.resolve[js.Any](context)
  }
}

object IOAzureHttpFunction {
  //case class AppObj(methods: js.Array[String], )
}