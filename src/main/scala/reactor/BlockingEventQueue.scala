// 899130 Tiainen, Toni

package reactor

import reactor.api.Event

final class BlockingEventQueue[T] (private val capacity: Int) {

  if (capacity <= 0) {
    throw new IllegalArgumentException("capacity must be positive")
  }

  val queue = Queue[T]()

  @throws[InterruptedException]
  def enqueue[U <: T](e: Event[U]): Unit = synchronized {
    while (queue.length == capacity) {
      wait()
    }

    queue.enqueue(e)
    notifyAll()
  }

  @throws[InterruptedException]
  def dequeue: Event[T] = synchronized {
    while (queue.isEmpty) {
      wait()
    }

    var e = queue.dequeue()
    notifyAll()
    e
  }

  def getAll: Seq[Event[T]] = ???

  def getSize: Int = queue.length

  def getCapacity: Int = capacity

}
