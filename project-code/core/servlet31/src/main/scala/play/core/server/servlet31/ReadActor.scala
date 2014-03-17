package play.core.server.servlet31

import akka.actor.Actor
import play.api.libs.iteratee.{Step, Iteratee}
import play.api.mvc.SimpleResult
import javax.servlet.ServletInputStream
import play.api.Logger
import play.api.libs.iteratee.Input.El
import scala.concurrent.Promise

class ReadActor(servletInputStream: ServletInputStream, bodyParser: Iteratee[Array[Byte], SimpleResult], result: Promise[SimpleResult]) extends Actor {
  import ReadActor._
  val exContext = play.api.libs.concurrent.Execution.defaultContext

  var parser = bodyParser

  def receive = {
    case DataAvailable =>
      Logger("play").error(s"will consume body")
      parser = parser.pureFlatFold { step =>
        val chunk = ReadActor.consumeBody(servletInputStream)
        if (servletInputStream.isFinished) {
          Logger("play").error(s"force AllDataRead")
          self ! AllDataRead
        }
        step match {
          case Step.Cont(k) => k(El(chunk))
          case other => other.it
        }
      }(exContext)

    case AllDataRead =>
      Logger("play").error(s"all data read")
      parser.run.map { r =>
        result.success(r)
      }(exContext)
  }
}

object ReadActor {
  object DataAvailable
  object AllDataRead
  object GiveResult
  case class NextStep(step: Step[Array[Byte], SimpleResult])

  private def consumeBody(body: ServletInputStream): Array[Byte] = {
    val buffer = new Array[Byte](1024 * 8)
    val output = new java.io.ByteArrayOutputStream()
    var continue = body.isReady
    while (continue) {
      val length = body.read(buffer)
      if (length == -1) {
        continue = false
      } else {
        output.write(buffer, 0, length)
        continue = body.isReady
        Logger("play.war.servlet31").error(s"consumed $length bytes, continue = $continue")
      }
    }
    output.toByteArray
  }
}
