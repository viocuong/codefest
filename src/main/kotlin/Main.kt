import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.runBlocking
import java.io.IOException

fun main(args: Array<String>): Unit = runBlocking {
    val botExecutor = BotExecutor()
    botExecutor.initGame(
        host = "http://localhost",
        playerInfo = PlayerInfo(gameId = "49db2638-d0be-4c65-8094-d06ca7656e09", playerId = "player2-xxx"),
    )
}