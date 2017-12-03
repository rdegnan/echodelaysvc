package org.squbs.echodelaysvc

import java.util.function.Consumer

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import org.scalatest.{AsyncFlatSpecLike, Matchers}
import org.squbs.echodelaysvc.proto.EchoDelayClient
import org.squbs.echodelaysvc.proto.service.EchoRequest
import org.squbs.testkit.CustomTestKit
import org.squbs.unicomplex.JMX
import reactor.core.scala.publisher.{Flux, Mono}
import reactor.ipc.netty.options.ClientOptions
import reactor.ipc.netty.tcp.TcpClient

import scala.language.postfixOps

object ProteusActorSpec {

  val config: Config = ConfigFactory.parseString(
    s"""
       |squbs {
       |  ${JMX.prefixConfig} = true
       |}
       |
       |default-listener.bind-port = 0
     """.stripMargin)
}

class ProteusActorSpec extends CustomTestKit(ProteusActorSpec.config) with AsyncFlatSpecLike with Matchers {

  implicit val _ = ActorMaterializer()

  val socket = RSocketFactory.connect()
    .transport(TcpClientTransport.create(8801))
    .start()
    .block()

  it should "respond to the echo requests" in {
    val client = new EchoDelayClient(socket)
    val responseFuture = Source(1 to 10)
      .flatMapMerge(10, i => Source.fromPublisher(client.echo(EchoRequest.toJavaProto(EchoRequest(i.toString)))))
      .runWith(Sink.seq)

    responseFuture.map { seq =>
      seq should have size 10
    }
  }
}
