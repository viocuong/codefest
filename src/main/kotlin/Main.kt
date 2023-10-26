import bot.BotExecutor
import bot.model.PlayerInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

fun main(args: Array<String>): Unit = runBlocking {

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "44740537-c2ea-461a-b059-b15fb5ddec91", playerId = "player1-xxx"),
        )
    }

    launch {
        val botExecutor = BotExecutor()
        botExecutor.initGame(
            host = "http://localhost",
            playerInfo = PlayerInfo(gameId = "44740537-c2ea-461a-b059-b15fb5ddec91", playerId = "player2-xxx"),
        )
    }
}