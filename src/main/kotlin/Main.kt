import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

data class Student(val name: String)
fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    println(map.size)
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "65d0a9ac-c28e-4816-a971-ed367b83b960", playerId = "player1-xxx"),
        )
    }
    

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "65d0a9ac-c28e-4816-a971-ed367b83b960", playerId = "player2-xxx"),
        )
    }
}