import bot.BotExecutor
import kotlinx.coroutines.runBlocking
import model.PlayerInfo

fun main(args: Array<String>): Unit = runBlocking {
    val botExecutor = BotExecutor()
    botExecutor.initGame(
        host = "http://localhost",
        playerInfo = PlayerInfo(gameId = "6ce18bd6-d2d3-446f-8628-30f3f47b4d01", playerId = "player2-xxx"),
    )
}