package org.squbs.echodelaysvc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import io.netifi.sdk.Netifi
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import org.squbs.echodelaysvc.proto.service.EchoRequest
import org.squbs.echodelaysvc.proto.EchoDelayClient

import scala.concurrent.duration._
import scala.language.postfixOps

class EchoDelayActorSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("EchoDelayActorSpec"))

  implicit val _system = system
  implicit val mat = ActorMaterializer()
  val actorRef = TestActorRef[EchoDelayActor]
  val actor = actorRef.underlyingActor
  implicit val timeout = Timeout(1 second)

  val accessKey = 3855261330795754807L
  val accessToken = "n9R9042eE1KaLtE56rbWjBIGymo="
  val socket = Netifi.builder()
    .accountId(Long.MaxValue)
    .accessKey(accessKey)
    .accessToken(accessToken)
    .group("echodelay.client")
    .destination("client")
    .build()
    .connect("echodelay.server")
    .block()

  override def afterAll() = {
    system.shutdown()
  }

  describe ("The EchoDelay actor") {
    it ("should provide confirmation for an echo request") {
      val client = new EchoDelayClient(socket)
      val response = client.echo(EchoRequest.toJavaProto(EchoRequest("foo"))).block()
      response.getPath should be ("foo")
    }
  }
}
