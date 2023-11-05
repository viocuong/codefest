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
            playerInfo = PlayerInfo(gameId = "396a88cc-6c22-4458-9558-b0b89df28054", playerId = "player1-xxx"),
        )
    }

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "396a88cc-6c22-4458-9558-b0b89df28054", playerId = "player2-xxx"),
        )
    }
}