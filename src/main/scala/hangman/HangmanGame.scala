// 123456 Familyname, Firstname

package hangman
import hangman.util.{AcceptHandle, TCPTextHandle}
import reactor.Dispatcher
import reactor.api.{EventHandler, Handle}
import java.net.Socket
import scala.collection.immutable.List

class HangmanGame(val hiddenWord: String, val initialGuessCount: Int, val port: Option[Int]) {

  var state = new GameState(hiddenWord, initialGuessCount, Set())
  var players: List[Player] = List()
  val dispatcher = new Dispatcher()

  val addConnection = (socket: Socket) => {
    val textHandle = new TCPTextHandle(socket)
    val newPlayer = new Player(textHandle)
    players = players :+ newPlayer
    dispatcher.addHandler(newPlayer.getHandler)
  }

  val acceptHandle: AcceptHandle = new AcceptHandle(port)
  val gameHandler = new Handler[Socket](acceptHandle, addConnection)

  def start() : Unit = {
    dispatcher.addHandler(gameHandler)
    dispatcher.handleEvents()
  }

  def checkEnd(): Unit = {
    if (state.isGameOver) {
      players.foreach(p => p.close())
      dispatcher.removeHandler(gameHandler)
      acceptHandle.close()
    }
  }

  def notifyAllPlayers(guesser: String, guess: Char) : Unit = players.foreach(p => p.notifyState(guesser, guess))

  class Player(val handle: TCPTextHandle) {

    var name: String = null
    private val handler = new Handler(handle, receive)
  
    def receive(msg: String) : Unit = {
      if (msg == null) {
        close()
        return
      }
  
      if (name == null) {
        name = msg
        handle.write("%s %d".format(state.getMaskedWord, state.guessCount))
      } else {
        val guessChar = msg.charAt(0)
        state = state.makeGuess(guessChar)
        notifyAllPlayers(name, guessChar)
        checkEnd()
      }
    }

    def close(): Unit = {
      players = players.filterNot(p => p == this)
      handle.close()
      dispatcher.removeHandler(handler)
    }

    def notifyState(guesser: String, guess: Char) : Unit = 
      handle.write("%c %s %d %s".format(guess, state.getMaskedWord, state.guessCount, guesser))
  
    def getHandler = handler
  }

}

object HangmanGame {

  def main(args: Array[String]): Unit = {
    val port:Option[Int] = if (args.length > 2) Some(args(2).toInt) else None
    val game = new HangmanGame(args(0), args(1).toInt, port)
    game.start()
    
    println("Running game...")
  }

}



class Handler[T](val h: Handle[T], val f: T => Unit) extends EventHandler[T] {
  override def getHandle: Handle[T] = h
  override def handleEvent(data: T): Unit = { f(data) }
}