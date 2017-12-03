package org.squbs.echodelaysvc

import akka.NotUsed
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.trueaccord.scalapb.json.JsonFormat
import org.squbs.echodelaysvc.proto.service.EchoResponse
import org.squbs.unicomplex.FlowDefinition
import org.squbs.util.ConfigUtil._

import scala.concurrent.duration.FiniteDuration

class EchoFlow extends FlowDefinition {

  implicit val askTimeout: Timeout =
    Timeout(context.system.settings.config.get[FiniteDuration]("akka.http.server.request-timeout"))

  val delayActor = Lookup("/user/echodelaysvc/delayactor")

  override def flow: Flow[HttpRequest, HttpResponse, NotUsed] =
    Flow[HttpRequest].mapAsync(1) { req =>
      (delayActor ? ScheduleRequest(System.nanoTime(), req.uri.path.tail.toString)).mapTo[EchoResponse]
    }.map { response =>
      HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, JsonFormat.toJsonString(response)))
    }
}
