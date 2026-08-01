package feral.functions.util

import AppConfig._
import scala.scalajs.js
import feral.functions.facade.JSRequest
import feral.functions.facade.InvocationContext

final case class AppConfig(
    methods: List[HttpMethod],
    authLevel: AuthLevel,
    route: AzureRoute,
    handlerFn: HandlerFnT) {
  def toJS: js.Object = {
    js.Dynamic
      .literal(
        methods = methodsToJS(methods),
        authLevel = authLevel.value,
        route = route.value,
        handler = handlerFn
      )
  }
}

object AppConfig {
  sealed trait HttpMethod {
    def value: String = {
      this match {
        case Get => "GET"
        case Post => "POST"
        case Put => "PUT"
        case Patch => "PATCH"
        case Delete => "DELETE"
      }
    }
  }
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Put extends HttpMethod
  case object Patch extends HttpMethod
  case object Delete extends HttpMethod

  def methodsToJS(list: List[HttpMethod]): js.Array[String] = {
    val dList = list.distinct.map(_.value)
    js.Array(dList: _*)
  }

  case class AuthLevel(value: String)
  val anonymous: AuthLevel = AuthLevel("anonymous")
  // add more?

  case class AzureRoute(value: String)
  val catchAll: AzureRoute = AzureRoute("{*path}")
  // add more?

  type HandlerFnT = js.Function2[JSRequest, InvocationContext, js.Promise[js.UndefOr[js.Any]]]

  def buildDefaultConfig(handlerFn: HandlerFnT) =
    AppConfig(List(Get, Post, Put, Patch, Delete), anonymous, catchAll, handlerFn)
}
