// 123456 Familyname, Firstname

package reactor

import reactor.api.{Event, EventHandler}

final class Dispatcher(private val queueLength: Int = 10) {
  require(queueLength > 0)

  @throws[InterruptedException]
  def handleEvents(): Unit = ???

  @throws[InterruptedException]
  def select[_]: Event[_] = ???

  def addHandler[T](h: EventHandler[T]): Unit = ???

  def removeHandler[T](h: EventHandler[T]): Unit = ???

}


final class WorkerThread[T](???) extends Thread {

  override def run(): Unit = ???

  def cancelThread(): Unit = ???

}
