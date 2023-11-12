import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    println(map.size)
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "a1f59acc-1cd3-4af9-9231-e3e7d52ecd30", playerId = "player1-xxx"),
        )
    }

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "a1f59acc-1cd3-4af9-9231-e3e7d52ecd30", playerId = "player2-xxx"),
        )
    }
}