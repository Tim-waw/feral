package feral.functions.facade

import scala.scalajs.js

import scala.collection.mutable.ListBuffer

@js.native
trait JSRequest extends js.Object {
  def method: String = js.native
  def url: String = js.native
  def headers: JSHeaders = js.native
}

@js.native
trait JSHeaders extends js.Object {
  def get(name: String): js.UndefOr[String] = js.native
  def keys(): js.Iterator[String] = js.native 
}

object JSHeaders {
  def keyList(h: JSHeaders): List[String] = {
    val acc = ListBuffer[String]()
    val itr = h.keys()
    var entity = itr.next()

    while(!entity.done) {
      acc.addOne(entity.value)
      entity = itr.next()
    }

    acc.result()
  }

  object Syntax {
    //syntax for method like calls???
  }
}

/* Parameters

method: Method.GET, Method.POST, etc.
uri: representation of the request URI
httpVersion: the HTTP version
headers: collection of Headers
body: fs2.Stream[F, Byte] defining the body of the request
attributes: Immutable Map used for carrying additional information in a type safe fashion */