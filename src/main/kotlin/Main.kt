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
//    launch {
//        val botExecutor = BotExecutor()
//        botExecutor.initGame(
//            host = "http://localhost",
//            clientInfo = PlayerInfo(gameId = "2f8ac26d-b672-45bd-8204-6970c1d82243", playerId = "player1-xxx"),
//            killMode = false,
//        )
//    }
//
    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://172.20.10.2",
            clientInfo = PlayerInfo(gameId = "d2db0ea1-86f5-4a65-8d47-94e09364aee1", playerId = "player2-xxx"),
            killMode = true
        )
    }
}