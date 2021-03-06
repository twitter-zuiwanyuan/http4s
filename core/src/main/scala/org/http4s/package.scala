package org

import scalaz.{Kleisli, EitherT, \/}

import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.util.CaseInsensitiveString
import scodec.bits.ByteVector

package object http4s { // scalastyle:ignore

  type AuthScheme = CaseInsensitiveString

  type EntityBody = Process[Task, ByteVector]

  def EmptyBody: EntityBody = Process.halt

  type DecodeResult[T] = EitherT[Task, DecodeFailure, T]

  val ApiVersion: Http4sVersion = Http4sVersion(BuildInfo.apiVersion._1, BuildInfo.apiVersion._2)

  type ParseResult[+A] = ParseFailure \/ A

  val DefaultCharset = Charset.`UTF-8`

  /**
   * A Service wraps a function of request type `A` to a Task that runs
   * to response type `B`.  By wrapping the [[Service]], we can compose them
   * using Kleisli operations.
   */
  type Service[A, B] = Kleisli[Task, A, B]

  /**
    * A [[Service]] that produces a Task to compute a [[Response]] from a
    * [[Request]].  An HttpService can be run on any supported http4s
    * server backend, such as Blaze, Jetty, or Tomcat.
    */
  type HttpService = Service[Request, Response]

  type AuthedService[T] = Service[AuthedRequest[T], Response]

  /* Lives here to work around https://issues.scala-lang.org/browse/SI-7139 */
  object HttpService {
    /**
      * Lifts a total function to an `HttpService`. The function is expected to
      * handle all requests it is given.  If `f` is a `PartialFunction`, use
      * `apply` instead.
      */
    def lift(f: Request => Task[Response]): HttpService = Service.lift(f)

    /** Lifts a partial function to an `HttpService`.  Responds with
      * [[org.http4s.Response.fallthrough]], which generates a 404, for any request
      * where `pf` is not defined.
      */
    def apply(pf: PartialFunction[Request, Task[Response]]): HttpService =
      lift(req => pf.applyOrElse(req, Function.const(Response.fallthrough)))

    @deprecated("Use Response.fallthrough instead", "0.15")
    val notFound: Task[Response] =
      Response.fallthrough

    val empty: HttpService =
      Service.const(Response.fallthrough)
  }

  object AuthedService {
    /**
      * Lifts a total function to an `HttpService`. The function is expected to
      * handle all requests it is given.  If `f` is a `PartialFunction`, use
      * `apply` instead.
      */
    def lift[T](f: AuthedRequest[T] => Task[Response]): AuthedService[T] = Service.lift(f)

    /** Lifts a partial function to an `AuthedService`.  Responds with
      * [[org.http4s.Response.fallthrough]], which generates a 404, for any request
      * where `pf` is not defined.
      */
    def apply[T](pf: PartialFunction[AuthedRequest[T], Task[Response]]): AuthedService[T] =
      lift(req => pf.applyOrElse(req, Function.const(Response.fallthrough)))

    val empty: HttpService =
      Service.const(Response.fallthrough)
  }

  type Callback[A] = Throwable \/ A => Unit

  /** A stream of server-sent events */
  type EventStream = Process[Task, ServerSentEvent]
}
