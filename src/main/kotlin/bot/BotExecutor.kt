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

class BotExecutor {
    private var gameInfo: GameInfo? = null
    private var botSocket: Socket? = null
    private var gameJob: Job? = null
    private var coroutineScope: CoroutineScope? = null
    private var botHandler: BotHandler = BotHandler()
    private var player: Player? = null
    private var competitor: Player? = null
    private var playerId: String? = null
    suspend fun initGame(host: String, playerInfo: PlayerInfo) {
        playerId = playerInfo.gameId
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
                    JsonConverter.fromJson<GameInfo>(
                        old,
                        GameTagAdapter(),
                        ItemAdapter(),
                        SpoilTypeAdapter()
                    )?.timestamp == JsonConverter.fromJson<GameInfo>(
                        new, GameTagAdapter(),
                        ItemAdapter(),
                        SpoilTypeAdapter()
                    )?.timestamp
                }
                ?.collect { data ->
                    println(data.toString())
                    JsonConverter.fromJson<GameInfo>(
                        data,
                        GameTagAdapter(),
                        ItemAdapter(),
                        SpoilTypeAdapter()
                    )?.let { gameInfo ->
                        this@BotExecutor.gameInfo = gameInfo
                        onReceiveGame(gameInfo)
                    }

                    // println("map info: ${gameInfo}")
                }
        }
    }

    private fun onReceiveGame(gameInfo: GameInfo) {
        when (gameInfo.tag) {
            GameTag.START_GAME -> {
                println("START GAME")
                startMove()
            }

            GameTag.PLAYER_STOP_MOVING -> {
                //startMove()
            }

            else -> {}
        }
    }

    private fun startMove(){
        gameInfo?:return
        val direction = botHandler.move(
            mapInfo = gameInfo!!.mapInfo,
            currentPosition = player?.currentPosition ?: return,
            targetPredicate = { position ->
                if (checkSpoilNeedGet(position, gameInfo!!.mapInfo.spoils)) {
                    true to Command.STOP
                }
                if (checkPositionIsNearBalk(
                        mapInfo = gameInfo!!.mapInfo,
                        position = player?.currentPosition ?: Position(0, 0)
                    )
                ) {
                    true to Command.BOMB
                }
                false to Command.STOP
            }
        ).toDirection().toJson()
        println("Direction is ${direction}")
        botSocket?.emit(Event.DRIVE.value, direction)
    }

    private fun checkPositionIsNearBalk(mapInfo: MapInfo, position: Position): Boolean {
        mapInfo.map ?: return false
        for (i in 0 until 4) {
            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
            if (mapInfo.map[nextPosition.row][nextPosition.col] == ItemType.BALK) {
                return true
            }
        }
        return false
    }

    // TODO update strategy, [DELAY_TIME_DRAGON_EGG, MYSTIC_DRAGON_EGG]
    private fun checkSpoilNeedGet(position: Position, spoils: List<Spoil>?): Boolean {
        spoils ?: return false
        if (spoils.any { it.spoilType == SpoilType.SPEED_DRAGON_EGG && it.row == position.row && it.col == position.col }) {
            return true
        }
        return spoils.any { it.spoilType == SpoilType.ATTACK_DRAGON_EGG && it.row == position.row && it.col == position.col }
    }

    private fun handleStartGame(gameInfo: GameInfo) {

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
        private val adapterCustoms = listOf(GameTagAdapter(), ItemAdapter(), SpoilTypeAdapter())
    }
}