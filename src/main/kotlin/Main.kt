import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

fun main(args: Array<String>): Unit = runBlocking {
//    val map = mapOf(Student("A") to 1, Student("B") to 2, Student("A") to 3)
//    //ln(map.size)
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "02d295e2-6429-4a26-b459-42b75305260a", playerId = "player1-xxx"),
            killMode = true,
        )
    }
//
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            clientInfo = PlayerInfo(gameId = "02d295e2-6429-4a26-b459-42b75305260a", playerId = "player2-xxx"),
            killMode = true
        )
    }
}