package info.rori.lunchbox.server.akka.scala.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.Http
import akka.http.marshalling.ToResponseMarshallable
import akka.http.model.HttpResponse
import akka.http.server.{Directives, Route}
import akka.stream.scaladsl.ImplicitFlowMaterializer
import akka.util.Timeout
import info.rori.lunchbox.server.akka.scala.ApplicationModule
import info.rori.lunchbox.server.akka.scala.service.api.v1.ApiRouteV1
import info.rori.lunchbox.server.akka.scala.service.feed.FeedRoute

import scala.concurrent.{Future, ExecutionContextExecutor}
import scala.concurrent.duration.DurationInt

object HttpService {
  val Name = "http"

  def props(interface: String, port: Int) = Props(new HttpService(interface, port)(5.seconds))
}

class HttpService(host: String, port: Int)(implicit askTimeout: Timeout)
  extends Actor
  with ImplicitFlowMaterializer
  with MaintenanceRoute
  with ApiRouteV1
  with FeedRoute {

  log.info(s"Starting server at $host:$port")
  log.info(s"To shutdown, send http://$host:$port/shutdown")

  Http(context.system).bind(host, port).startHandlingWith(route)

  def route: Route = apiRouteV1 ~ feedRoute ~ maintenanceRoute

  override def receive = {
    case msg => log.warning("unhandled message in HttpService: " + msg)
  }
}


trait HttpRoute
  extends Actor
  with ActorLogging
  with Directives {

  val NotFound = akka.http.model.StatusCodes.NotFound
  val InternalServerError = akka.http.model.StatusCodes.InternalServerError

  implicit val timeout: Timeout = 1.second // Timeout for domain actor calls in path

  implicit def executor: ExecutionContextExecutor = context.dispatcher

  //  implicit val materializer: FlowMaterializer = ActorFlowMaterializer() // necessary for unmarshelling !?

  implicit class RichFutureToResponseMarshallable(val f: Future[ToResponseMarshallable]) {
    def recoverOnError(message: String) = f.recover[ToResponseMarshallable] {
      case exc: Throwable =>
        log.error(exc, s"HTTP call: $message")
        InternalServerError
    }
  }

}


/**
 * Stellt Wartungsroutinen über die HTTP-Schnittstelle bereit.
 */
trait MaintenanceRoute
  extends HttpRoute {

  def maintenanceRoute =
    logRequest(context.system.name) {
      path("shutdown") {
        get {
          complete {
            // TODO: Shutdown an ApplicationRoot schicken
            context.system.scheduler.scheduleOnce(500.millis, self, ApplicationModule.Shutdown)
            log.info("Shutting down now ...")
            HttpResponse(entity = "Shutting down now ...")
          }
        }
      }
    }
}

