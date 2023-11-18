import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*

fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    println(map.size)
//    launch {
//        val botExecutor = BotExecutor()
//        botExecutor.initGame(
//            host = "http://localhost",
//            clientInfo = PlayerInfo(gameId = "535c80fb-b3c2-4c0e-b4c3-f81a6d3f45b5", playerId = "player1-xxx"),
//        )
//    }
//
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "359a943e-c5fe-4331-a58c-ed1070275bfe", playerId = "player2-xxx"),
        )
    }
}