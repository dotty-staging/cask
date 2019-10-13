package cask.util

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Promise}

class WsClient(impl: WebsocketBase)
              (implicit ec: ExecutionContext, log: Logger)
  extends cask.util.BatchActor[Ws.Event]{

  def run(items: Seq[Ws.Event]): Unit = items.foreach{
    case Ws.Text(s) => impl.send(s)
    case Ws.Binary(s) => impl.send(s)
    case Ws.Close(_, _) => impl.close()
    case Ws.ChannelClosed() => impl.close()
  }
}

object WsClient{
  def connect(url: String)
             (f: PartialFunction[cask.util.Ws.Event, Unit])
             (implicit ec: ExecutionContext, log: Logger): WsClient = {
    Await.result(connectAsync(url)(f), Duration.Inf)
  }
  def connectAsync(url: String)
                  (f: PartialFunction[cask.util.Ws.Event, Unit])
                  (implicit ec: ExecutionContext, log: Logger): scala.concurrent.Future[WsClient] = {
    object receiveActor extends cask.util.BatchActor[Ws.Event] {
      def run(items: Seq[Ws.Event]) = items.foreach(x => f.applyOrElse(x, (_: Ws.Event) => ()))
    }
    val p = Promise[WsClient]
    val impl = new WebsocketClientImpl(url) {
      def onOpen() = {
        if (!p.isCompleted) p.success(new WsClient(this))
      }
      def onMessage(message: String) = {
        receiveActor.send(Ws.Text(message))
      }
      def onMessage(message: Array[Byte]) = {
        receiveActor.send(Ws.Binary(message))
      }
      def onClose(code: Int, reason: String) = {
        if (!p.isCompleted) p.failure(new Exception(s"WsClient failed: $code $reason"))
        else receiveActor.send(Ws.Close(code, reason))
      }
      def onError(ex: Exception): Unit = {
        if (!p.isCompleted) p.failure(ex)
        else receiveActor.send(Ws.Error(ex))
      }
    }

    impl.connect()
    p.future
  }
}
