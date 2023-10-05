package bot

import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import model.*
import utils.JsonConverter
import utils.JsonConverter.toJson
import utils.onFLow
import java.util.LinkedList
import java.util.Queue

enum class Event(val value: String) {
    JOIN_GAME("join game"),
    TICK_TACK("ticktack player"),
    DRIVE("drive player")
}


//class FullAttack : AttackStrategy() {
//    override fun attack(command: List<Command>) {
//
//    }
//}

class BotExecutor {
    private var gameInfo: GameInfo? = null
    private var botSocket: Socket? = null
    private var gameJob: Job? = null
    private var coroutineScope: CoroutineScope? = null
    suspend fun initGame(host: String, playerInfo: PlayerInfo) {
        coroutineScope {
            BotSocket.initSocket(host = host) {
                botSocket = this
                on(Event.JOIN_GAME.value) { args ->
                    val data = args[0]
                    println("Join game $data")
                }
                on(Socket.EVENT_CONNECT) {
                    println("connected")
                    emit(
                        Event.JOIN_GAME.value,
                        playerInfo.toJson()
                    )
                }
            }
            botSocket?.onFLow(Event.TICK_TACK.value)
                ?.distinctUntilChanged { old, new ->
                    JsonConverter.fromJson<GameInfo>(old)?.timestamp == JsonConverter.fromJson<GameInfo>(new)?.timestamp
                }
                ?.collect { data ->
                    println(data.toString())
                    gameInfo = JsonConverter.fromJson<GameInfo>(
                        data,
                        GameTagAdapter(),
                        ItemAdapter(),
                    )
                    if (gameInfo?.tag == GameTag.START_GAME) {
                        playBot()
                    }

                    // println("map info: ${gameInfo}")
                }
        }
    }

    private suspend fun playBot() {
        while (true) {
            println(moveBot().toDirection().toJson().toString())
            botSocket?.emit(Event.DRIVE.value, moveBot().toDirection().toJson())
            return
        }
    }

    private fun moveBot(): List<Command> {
        return listOf(Command.RIGHT, Command.RIGHT, Command.DOWN, Command.BOMB)
    }

    companion object {
        private const val DELAY_INTERVAL = 500L
    }
}