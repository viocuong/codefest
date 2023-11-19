import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*

fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    //ln(map.size)
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "fe0e4970-5737-4a7d-b664-c8312bf86ffe", playerId = "player1-xxx"),
        )
    }
//
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "fe0e4970-5737-4a7d-b664-c8312bf86ffe", playerId = "player2-xxx"),
        )
    }
}