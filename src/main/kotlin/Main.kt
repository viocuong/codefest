import bot.BotExecutor
import kotlinx.coroutines.runBlocking
import model.PlayerInfo

fun main(args: Array<String>): Unit = runBlocking {
    val botExecutor = BotExecutor()
    botExecutor.initGame(
        host = "http://localhost",
        playerInfo = PlayerInfo(gameId = "564a1913-7a7d-4145-83c8-5f53340bc98a", playerId = "player2-xxx"),
    )
}