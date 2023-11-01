package bot

import bot.model.*
import bot.strategys.*
import io.socket.client.Socket
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import utils.JsonConverter
import utils.JsonConverter.toJson
import utils.onFLow

enum class Event(val value: String) {
    JOIN_GAME("join game"),
    TICK_TACK("ticktack player"),
    DRIVE("drive player")
}

class BotExecutor {
    private var botSocket: Socket? = null
    private var dropBombLastTime = 0L
    private val bombManager = BombManager()
    private var allBomb = mutableListOf<Bomb>()
    private var bombs: MutableList<MutableList<Long>> = mutableListOf()
    suspend fun initGame(host: String, playerInfo: PlayerInfo) {
        BotSocket.initSocket(host = host) {
            botSocket = this
            on(Event.JOIN_GAME.value) { args ->
                val data = args[0]
                println("Join game $data")
            }
            on(Socket.EVENT_CONNECT) {
                emit(
                    Event.JOIN_GAME.value,
                    playerInfo.toJson()
                )
            }
        }
        botSocket?.onFLow(Event.TICK_TACK.value)
            ?.map { json ->
                println(json)
                JsonConverter.fromJson<GameInfo>(
                    json,
                    GameTagAdapter(),
                    ItemAdapter(),
                    SpoilTypeAdapter()
                )
            }
            ?.filterNotNull()
            ?.collect { gameInfo ->
                onReceiveGame(
                    gameInfo.copy(
                        playerId = playerInfo.playerId,
                        bombManager = bombManager,
                        bombs = bombs
                    )
                )
            }
    }

    private suspend fun onReceiveGame(gameInfo: GameInfo) {
        if (bombs.isEmpty()) {
            bombs.addAll(List(gameInfo.mapInfo.size.rows) { MutableList(gameInfo.mapInfo.size.cols) { 0 } })
        }
        when (gameInfo.tag) {
            GameTag.PLAYER_BACK_TO_PLAY -> {
            }

            GameTag.START_GAME -> {
                targetPosition = gameInfo.player.currentPosition
            }

            GameTag.PLAYER_STOP_MOVING -> {
            }

            GameTag.UPDATE_DATA -> {
            }

            GameTag.BOMB_EXPLOSED -> {
//                val bombsExposed = currentBombs - gameInfo.mapInfo.bombs.toSet()
//                bombsExposing.addAll(bombsExposed.map { it.copy(endExposedTime = gameInfo.timestamp + 2000 + COMPLETE_EXPOSED_TIME) })
//                println("boms exposing = ${bombsExposing.filter { gameInfo.timestamp < it.endExposedTime }}")
            }

            GameTag.BOMB_SETUP -> {
                if (gameInfo.isActionOfPlayer) {
                    dropBombLastTime = gameInfo.timestamp
                }
                gameInfo.mapInfo.bombs.forEach { bomb ->
                    val exposedEndTime = bomb.remainTime + gameInfo.timestamp + COMPLETE_EXPOSED_TIME
                    println("timestamp = ${gameInfo.timestamp}, exposedTime = ${exposedEndTime}")
                    bombs[bomb.row][bomb.col] = maxOf(bombs[bomb.row][bomb.col], exposedEndTime)
                }
                println("BOM SETUP")
            }

            else -> {}
        }
        lastTime = gameInfo.timestamp
        startMove(gameInfo)
    }

    private var lastTime = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    private suspend fun startMove(gameInfo: GameInfo) {
        // Check if player is dangerous, and move to safe zone.
        println("player = ${gameInfo.player}, currentPosition = ${gameInfo.player.currentPosition}, boms = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb()) {
            val safeCommands =
                BotHandler.move(
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombStrategy(),
                    isNearBomb = true,
                )
            val bestAndSafeCommand =
                BotHandler.move(
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombAndGetSpoil(),
                )
            println(
                """
                bestAndSafeCommand = $bestAndSafeCommand
                safeCommands = $safeCommands
            """.trimIndent()
            )
            val command =
                if (bestAndSafeCommand.isNotEmpty() && (safeCommands.isEmpty() || bestAndSafeCommand.size < safeCommands.size)) {
                    bestAndSafeCommand.firstOrNull()
                } else {
                    safeCommands.firstOrNull()
                }
            println("Avoid BOMB direct" + safeCommands)
            sendCommand(command)
            return
        }

        // Move to an advantageous position
        val dropBombDirections =
            BotHandler.move(
                gameInfo = gameInfo,
                targetPredicate = DropBombStrategy(dropBombLastTime),
            )
        val getSpoilDirections =
            BotHandler.move(gameInfo = gameInfo, targetPredicate = GetSpoilsStrategy())
        println("DROP bomb = $dropBombDirections")
        println("GET spoil = $getSpoilDirections")
        // If can't get spoil and drop bomb, perform attack competitor egg.
        if (dropBombDirections.isEmpty() && getSpoilDirections.isEmpty()) {
            println("Attack competitor egg")
            val directionsAttach =
                BotHandler.move(
                    gameInfo = gameInfo,
                    targetPredicate = AttackCompetitorEggStrategy(dropBombLastTime),
                )
            sendCommand(directionsAttach.firstOrNull())
            return
        }
        val command =
            if (getSpoilDirections.isNotEmpty() && (dropBombDirections.isEmpty() || getSpoilDirections.size <= dropBombDirections.size)) {
                println("GET SPOIL = $getSpoilDirections")
                getSpoilDirections.firstOrNull()
            } else {
                println("DROP BOMB = $dropBombDirections")
                dropBombDirections.firstOrNull()
            }
        sendCommand(command)
    }

    private fun sendCommand(command: Command?) {
        isMoving = false
        println("COMMAND = $command")
        command ?: return
        botSocket?.emit(Event.DRIVE.value, Direction(command.value).toJson())
    }

    companion object {
        const val COMPLETE_EXPOSED_TIME = 600L
    }
}