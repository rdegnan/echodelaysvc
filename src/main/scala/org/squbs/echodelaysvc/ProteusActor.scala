package org.squbs.echodelaysvc

import java.util.function.Consumer

import akka.actor.Actor
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.{ConnectionSetupPayload, RSocket, RSocketFactory, SocketAcceptor}
import org.reactivestreams.Publisher
import org.squbs.echodelaysvc.proto.service.EchoResponse
import org.squbs.echodelaysvc.proto.{EchoDelay, EchoDelayServer, EchoRequest}
import reactor.core.publisher.Flux
import reactor.core.scala.publisher.Mono
import reactor.ipc.netty.options.ServerOptions
import reactor.ipc.netty.tcp.TcpServer
import org.squbs.util.ConfigUtil._

import scala.concurrent.duration.FiniteDuration

class ProteusActor extends EchoDelay with Actor {
  import context.dispatcher
  implicit val mat = ActorMaterializer()

  implicit val askTimeout: Timeout =
    Timeout(context.system.settings.config.get[FiniteDuration]("akka.http.server.request-timeout"))

  val delayActor = Lookup("/user/echodelaysvc/delayactor")
  val handler: RSocket = new EchoDelayServer(this)
  val tcpServer = TcpServer.builder()
    .options(new Consumer[ServerOptions.Builder[_ <: ServerOptions.Builder[_]]] {
      override def accept(options: ServerOptions.Builder[_ <: ServerOptions.Builder[_]]): Unit = {
        options.eventLoopGroup(new NioEventLoopGroup(0, context.dispatcher))
        options.port(8801)
      }
    }).build()

  val server = RSocketFactory.receive()
    .acceptor(new SocketAcceptor {
      override def accept(setup: ConnectionSetupPayload, sendingSocket: RSocket) = Mono.just(handler).asJava()
    })
    .transport(TcpServerTransport.create(tcpServer))
    .start()
    .block()

  override def echo (message: EchoRequest, metadata: ByteBuf) = {
    Mono.fromFuture((delayActor ? ScheduleRequest(System.nanoTime(), message.getPath)).mapTo[EchoResponse])
      .map(EchoResponse.toJavaProto(_))
      .asJava()
  }

  override def echoStream(messages: Publisher[EchoRequest], metadata: ByteBuf) = {
    val publisher = Source.fromPublisher(messages)
      .mapAsync(10) { message =>
        (delayActor ? ScheduleRequest(System.nanoTime(), message.getPath)).mapTo[EchoResponse]
      }
      .map(EchoResponse.toJavaProto(_))
      .runWith(Sink.asPublisher(fanout = false))

    Flux.from(publisher)
  }

  def receive = {
    case _ =>
  }
}
