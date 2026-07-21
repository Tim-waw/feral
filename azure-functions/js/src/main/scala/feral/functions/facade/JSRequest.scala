package feral.functions.facade

import scala.scalajs.js

//may want to change to requestFacade.., think it over

@js.native
trait JSRequest extends js.Object {
  def method: String = js.native
  def url: String = js.native
  def headers: JSHeaders = js.native
  def body: JSReadableStream = js.native
}

@js.native
trait JSHeaders extends js.Object {
  def get(name: String): js.UndefOr[String] = js.native
  def keys(): js.Iterator[String] = js.native 
}

@js.native
trait JSReadableStream extends js.Object

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

/* Parameters

method: Method.GET, Method.POST, etc.
uri: representation of the request URI
httpVersion: the HTTP version //not used in lambda-http4s
headers: collection of Headers
body: fs2.Stream[F, Byte] defining the body of the request
attributes: Immutable Map used for carrying additional information in a type safe fashion //not used in lambda-http4s
*/