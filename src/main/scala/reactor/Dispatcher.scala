// 899130 Tiainen, Toni

package reactor

import reactor.api.{Event, EventHandler}
import scala.collection.mutable.Map

final class Dispatcher(private val queueLength: Int = 10) {
  require(queueLength > 0)
  val queue = new BlockingEventQueue[Any](queueLength)
  var handlers:Map[EventHandler[_], WorkerThread[_]] = Map()

  @throws[InterruptedException]
  def handleEvents(): Unit = {
    try {

      // Main event loop:
      // Gets events from the queue one-by-one,
      // dispatches them to the related event handler if the data is not null.
      // If the data is null, the event handler is removed.
      while (!handlers.isEmpty) {
        var event = select
        if (event.getEvent == null) {
          removeHandler(event.getHandler)
        } else {
          event.handle()  
        }
      }
    } catch {
      case e: InterruptedException => {
        // If the main thread is interrupted, we remove all the handlers and
        // terminate the handle threads
        handlers.keys.foreach(removeHandler(_))
        throw e
      }
    }
    
  }

  @throws[InterruptedException]
  def select[_]: Event[_] = {
    try {
      queue.dequeue
    } catch {
      case e: InterruptedException => throw e
    }
    
  }

  def addHandler[T](h: EventHandler[T]): Unit = synchronized {
    var handleThread = new WorkerThread[T](h, queue)
    handlers += (h -> handleThread)

    // Handle thread starts reading messages
    handleThread.start()
  }

  def removeHandler[T](h: EventHandler[T]): Unit = synchronized {
    var handleThread = handlers.remove(h).get
    handleThread.interrupt()
  }

}



final class WorkerThread[T](handler: EventHandler[T], queue: BlockingEventQueue[Any]) extends Thread {

  override def run(): Unit = {
    var encounteredNull = false

    // Handle reading loop:
    // Thread keeps reading the messages returned by the handle,
    // and stops if it encounters null.
    // Makes sure that the null-message is the last event pushed to the queue
    // for this handler, so it can be safely deregistered and removed. 
    while (!encounteredNull) {
      var data = handler.getHandle.read()
      queue.enqueue(new Event[T](data, handler))

      // If the data is null, we stop receiving the messages
      // Note: Event with data: null is still pushed to the queue because
      //  we need to tell the dispatcher to deregister the event handler
      if (data == null) {
        encounteredNull = true
      }
    }
  }

  // This was included in the template, but I didn't use it, so
  // it is commented out. The dispatcher should be responsible for terminating
  // registered threads.
  /*
  def cancelThread(): Unit = {
    this.interrupt()
  }
  */
}
