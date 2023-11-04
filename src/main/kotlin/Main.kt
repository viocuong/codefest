import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    println(map.size)
//    launch {
//        val botExecutor = BotExecutor()
//        botExecutor.initGame(
//            host = "http://localhost",
//            playerInfo = PlayerInfo(gameId = "cc92e719-e0c0-401a-8c93-2b3d6eb0fb14", playerId = "player1-xxx"),
//        )
//    }

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "8b759bab-5419-417f-92a3-172b096a343c", playerId = "player2-xxx"),
        )
    }
}