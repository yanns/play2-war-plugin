package play.core.server.servlet31

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import play.core.server.servlet.{RichHttpServletResponse, RichHttpServletRequest, Play2GenericServletRequestHandler}
import java.io.{OutputStream, InputStream}
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.{ServletInputStream, ReadListener, AsyncEvent}
import play.api.Logger
import play.api.libs.iteratee._
import scala.concurrent.{Future, Promise}
import play.api.libs.iteratee.Input.El
import play.api.mvc.SimpleResult
import scala.concurrent.stm.Ref

class Play2Servlet31RequestHandler(servletRequest: HttpServletRequest)
  extends Play2GenericServletRequestHandler(servletRequest, None)
  with Helpers {

  val asyncListener = new AsyncListener(servletRequest.toString)

  val asyncContext = servletRequest.startAsync
  asyncContext.setTimeout(play.core.server.servlet31.Play2Servlet.asyncTimeout)

  protected override def onFinishService() = {
    // Nothing to do
  }

  protected override def onHttpResponseComplete() = {
    asyncContext.complete()
  }

  protected override def getHttpRequest(): RichHttpServletRequest = {
    new RichHttpServletRequest {
      def getRichInputStream(): Option[InputStream] = {
        if (asyncContextAvailable(asyncListener)) {
          Option(asyncContext.getRequest.getInputStream)
        } else {
          None
        }
      }
    }
  }

  protected override def getHttpResponse(): RichHttpServletResponse = {
    new RichHttpServletResponse {
      def getRichOutputStream: Option[OutputStream] = {
        if (asyncContextAvailable(asyncListener)) {
          Option(asyncContext.getResponse.getOutputStream)
        } else {
          None
        }
      }

      def getHttpServletResponse: Option[HttpServletResponse] = {
        if (asyncContextAvailable(asyncListener)) {
          Option(asyncContext.getResponse.asInstanceOf[HttpServletResponse])
        } else {
          None
        }
      }
    }
  }

  private def asyncContextAvailable(asyncListener: AsyncListener) = {
    !asyncListener.withError.get && !asyncListener.withTimeout.get
  }

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

  private def readServletRequest[A](servletInputStream: ServletInputStream, consumer: => Iteratee[Array[Byte], A]): Future[A] = {
    implicit val exContext = play.api.libs.concurrent.Execution.defaultContext
    //val exContext = play.core.Execution.Implicits.internalContext

    val result = Promise[A]()
    val iterateeRef = Ref[Future[Iteratee[Array[Byte], A]]](Future.successful(consumer))

    val readListener = new ReadListener {

      def onDataAvailable() {
        Logger("play.war.servlet31").error(s"onDataAvailable begin")
        val nextIteratee = Promise[Iteratee[Array[Byte], A]]()
        iterateeRef.single.transform { fCurrent =>
          val current = Iteratee.flatten(fCurrent)
          nextIteratee.success(current.pureFlatFold { step =>
            Logger("play").error(s"will consume body")
            Thread.sleep(200)
            val chunk = consumeBody(servletInputStream)
            val ready = servletInputStream.isReady
            val finished = servletInputStream.isFinished
            Logger("play.war.servlet31").error(s"consumes ${chunk.length} bytes - servletInputStream.isReady=$ready - servletInputStream.isFinished=$finished")
            step match {
              case Step.Cont(k) =>
                Logger("play.war.servlet31").error(s"cont - consumes ${chunk.length} bytes")
                k(El(chunk))
              case other =>
                Logger("play.war.servlet31").error(s"$other - consumes ${chunk.length} bytes")
                other.it
            }
          })
          nextIteratee.future
        }
        Logger("play.war.servlet31").error(s"onDataAvailable end")
      }

      def onAllDataRead() {
        Logger("play.war.servlet31").error("onAllDataRead")
        result.completeWith(for {
          it <- iterateeRef.single.get
          a <- it.run
        } yield a)
      }

      def onError(t: Throwable) {
        Logger("play.war.servlet31").error("onError", t)
        result.failure(t)
      }
    }

    servletInputStream.setReadListener(readListener)
    Logger("play.war.servlet31").error("servletInputStream readListener initialized")
    result.future
  }

  override protected def feedBodyParser(bodyParser: Iteratee[Array[Byte], SimpleResult]): Future[SimpleResult] = {
    val servletInputStream = servletRequest.getInputStream
    readServletRequest(servletInputStream, bodyParser)
  }
}

private[servlet31] class AsyncListener(val requestId: String) extends javax.servlet.AsyncListener {

  val withError = new AtomicBoolean(false)

  val withTimeout = new AtomicBoolean(false)

  // Need a default constructor for JBoss
  def this() = this("Unknown request id")

  override def onComplete(event: AsyncEvent): Unit = {
    // Logger("play").trace("onComplete: " + requestId)
    // Nothing
  }

  override def onError(event: AsyncEvent): Unit = {
    withError.set(true)
    Logger("play").error("Error asynchronously received for request: " + requestId, event.getThrowable)
  }

  override def onStartAsync(event: AsyncEvent): Unit = {} // Nothing

  override def onTimeout(event: AsyncEvent): Unit = {
    withTimeout.set(true)
    Logger("play").warn("Timeout asynchronously received for request: " + requestId)
  }
}
