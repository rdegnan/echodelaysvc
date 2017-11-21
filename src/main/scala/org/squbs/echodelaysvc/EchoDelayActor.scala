package org.squbs.echodelaysvc

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import io.netifi.sdk.Netifi
import org.squbs.echodelaysvc.proto.service.EchoResponse
import org.squbs.echodelaysvc.proto.{EchoDelay, EchoDelayServer, EchoRequest}
import reactor.core.scala.publisher.Mono

import scala.concurrent.duration.FiniteDuration

class EchoDelayActor extends EchoDelay with Actor {
  import context.dispatcher
  import org.squbs.util.ConfigUtil._

  implicit val askTimeout =
    Timeout(context.system.settings.config.get[FiniteDuration]("akka.http.server.request-timeout"))

  val delayActor = context.actorOf(Props[DelayActor])
  val accessKey = 3855261330795754807L
  val accessToken = "n9R9042eE1KaLtE56rbWjBIGymo="
  val server = Netifi.builder()
    .accountId(Long.MaxValue)
    .accessKey(accessKey)
    .accessToken(accessToken)
    .group("echodelay.server")
    .destination("server")
    .build()
    .addService(new EchoDelayServer(this))

  override def echo (message: EchoRequest) = {
    Mono.fromFuture((delayActor ? ScheduleRequest(System.nanoTime(), message.getPath)).mapTo[EchoResponse])
      .map(EchoResponse.toJavaProto(_))
      .asJava()
  }

  def receive = {
    case _ =>
  }
}