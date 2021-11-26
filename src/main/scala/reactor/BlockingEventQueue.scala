// 899130 Tiainen, Toni

package reactor

import reactor.api.Event
import scala.collection.mutable.Queue

final class BlockingEventQueue[T] (private val capacity: Int) {

  if (capacity <= 0) {
    throw new IllegalArgumentException("Capacity must be positive.")
  }

  val queue = Queue[Event[T]]()

  @throws[InterruptedException]
  def enqueue[U <: T](e: Event[U]): Unit = {

    // Synchonized to make sure if a thread is released from the
    // while condition, it must modify the queue before any other thread
    // is allowed to try releasing.
    try synchronized {

      // While condition makes sure that this thread is released only
      // when there is room in the queue.
      while (queue.length == capacity) {
        wait()
      }

      // Since the queue support all the sub-classes of T, denoted here by U,
      // we can store them as an instance of the class T, hence Evemt[T]
      queue.enqueue(e.asInstanceOf[Event[T]])

      // We use notifyAll, because notifying just one thread might
      // wake another enqueue thread, which might not able to continue.
      notifyAll()
    } catch {
      case e: InterruptedException => throw e
    }
    
  }

  @throws[InterruptedException]
  def dequeue: Event[T] = {

    // Same as with enqueueing, this needs to be synchronized
    // to prevent situations where multiple threads are released
    // before making changes to the queue.
    try synchronized {

      // While condition makes sure this thead is released only when 
      // queue has elements in it. 
      while (queue.isEmpty) {
        wait()
      }

      var e = queue.dequeue()
      notifyAll()
      e
    } catch {
      case e: InterruptedException => throw e
    }
  }

  // Converts the queue to sequence and clears the queue.
  // Syncrhonized guarantees that no events that aren't
  // included in the sequence are removed.
  def getAll: Seq[Event[T]] = synchronized {
    var sequence = queue.toSeq
    queue.clear()
    // Because the queue is modified, it would be wise to notify
    // threads that might be waiting for room to enqueue.
    // notifyAll()
    sequence
  }

  def getSize: Int = queue.length

  def getCapacity: Int = capacity

}
