package bot

import bot.model.*
import bot.strategys.*
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import utils.JsonConverter
import utils.JsonConverter.toJson
import utils.onFLow
import java.util.logging.Logger

enum class Event(val value: String) {
    JOIN_GAME("join game"),
    TICK_TACK("ticktack player"),
    DRIVE("drive player")
}

class BotExecutor {
    private var botSocket: Socket? = null
    private var dropBombLastTime = 0L
    private var bombs: MutableList<MutableList<Long>> = mutableListOf()
    private var bombManager: BombManager = BombManager()
    private var playerId: String = ""
    private var isMock = false
    suspend fun initGame(host: String, clientInfo: PlayerInfo, killMode: Boolean = false, isMock: Boolean = false) {
        this.isMock = isMock
        BotSocket.initSocket(host = host) {
            botSocket = this
            on(Event.JOIN_GAME.value) { args ->
                val gameInfo = JsonConverter.fromJson<PlayerInfo>(args[0] as JSONObject)
                if (clientInfo.playerId?.contains(gameInfo?.playerId ?: "") == true) {
                    playerId = gameInfo?.playerId ?: ""
                }
//                //ln("Join game $data")
            }
            on(Socket.EVENT_CONNECT) {
                emit(
                    Event.JOIN_GAME.value, clientInfo.toJson()
                )
            }
        }
        botSocket?.onFLow(Event.TICK_TACK.value)
            ?.map { json ->
//                //ln(json)
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
                        playerId = playerId,
                        bombManager = bombManager,
                    ),
                    killMode = killMode
                )
            }
    }

    private var bombCurrent: Position = Position.NONE

    private suspend fun onReceiveGame(gameInfo: GameInfo, killMode: Boolean) {
        if (isMock) return
        bombManager.init(gameInfo.mapInfo.size)
//        log.info("GAME TAG = ${gameInfo.tag}")
        when (gameInfo.tag) {
            GameTag.PLAYER_BACK_TO_PLAY -> {
//                if (gameInfo.isActionOfPlayer) {
//                    startMove(gameInfo)
//                }
            }

            GameTag.START_GAME -> {
                targetPosition = gameInfo.player.currentPosition
//                if (gameInfo.isActionOfPlayer) {
//                    startMove(gameInfo)
//                }
            }

            GameTag.PLAYER_STOP_MOVING -> {
//                if (gameInfo.isActionOfPlayer) {
//                    startMove(gameInfo, killMode)
//                }
            }

            GameTag.UPDATE_DATA -> {
            }


            GameTag.BOMB_EXPLOSED -> {
//                val bombsExposed = currentBombs - gameInfo.mapInfo.bombs.toSet()
//                bombsExposing.addAll(bombsExposed.map { it.copy(endExposedTime = gameInfo.timestamp + 2000 + COMPLETE_EXPOSED_TIME) })
//                //ln("boms exposing = ${bombsExposing.filter { gameInfo.timestamp < it.endExposedTime }}")
            }

            GameTag.BOMB_SETUP -> {
                if (gameInfo.isActionOfPlayer) {
                    dropBombLastTime = gameInfo.timestamp
                    bombCurrent = gameInfo.player.currentPosition
                }
                bombManager.updateBombs(gameInfo)
//                //ln("BOM SETUP")
            }

            else -> {}
        }
//        if (gameInfo.timestamp - lastTime > 50) {
//        sendCommand(Command.STOP, gameInfo)
        lastTime = gameInfo.timestamp
        startMove(gameInfo, killMode)
//        }
    }

    private var lastTime = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    private suspend fun startMove(gameInfo: GameInfo, killMode: Boolean) = coroutineScope {
        // Check if player is dangerous, and move to safe zone.
//        //ln("player = ${gameInfo.player}, currentPosition = ${gameInfo.player.currentPosition}, boms = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb(noCheckTime = true)) {
            moveToSaveZone(gameInfo)
            return@coroutineScope
        }

        val dropBombDirections = getDirectionsDropBomb(gameInfo)
        val getSpoilDirections =
            BotHandler.move(
                position = gameInfo.player.currentPosition,
                gameInfo = gameInfo,
                targetPredicate = GetSpoilsStrategy(dropBombLastTime),
            )
        if (dropBombDirections.isEmpty() && getSpoilDirections.isEmpty()) {
//            log.info("Attack competitor egg")
            val killCompetitorDirections = BotHandler.move(
                position = gameInfo.player.currentPosition,
                gameInfo = gameInfo,
                targetPredicate = KillCompetitorStrategy(dropBombLastTime),
            )
            val directionsAttach =
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = AttackCompetitorEggStrategy(dropBombLastTime),
                )
            val attackCommand = if (killMode && gameInfo.player.score <= gameInfo.competitor.score) {
                killCompetitorDirections.firstOrNull()
            } else {
                directionsAttach.firstOrNull()
            }

            sendCommand(attackCommand, gameInfo)
            return@coroutineScope
        }
        val command =
            if (
                getSpoilDirections.firstOrNull() == Command.BOMB ||
                (
                        getSpoilDirections.isNotEmpty() && dropBombDirections.isNotEmpty() &&
                                getSpoilDirections.size <= (dropBombDirections.size * 3)
                            .coerceAtLeast(5)
                            .coerceAtMost(10))
            ) {
                println("GET SPOIL = $getSpoilDirections")
                getSpoilDirections.firstOrNull()
            } else if (dropBombDirections.isNotEmpty()) {
                println("DROP BOMB = $dropBombDirections")
                dropBombDirections.firstOrNull()
            } else {
                listOf(getSpoilDirections, dropBombDirections).firstOrNull { it.isNotEmpty() }?.firstOrNull()
            }
        sendCommand(command, gameInfo)
    }

    private suspend fun moveToSaveZone(gameInfo: GameInfo) = coroutineScope {
        val timeOfBombCanMoveOver =
            gameInfo.getBombsAtPosition(gameInfo.player.currentPosition).maxOfOrNull { it.remainTime } ?: 0
        val (safeCommands, bestAndSafeCommand, safeLast) = awaitAll(
            async(Dispatchers.Default) {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombStrategy(),
                    isNearBomb = true,
                    noCheckTimeOfBomb = true,
                    timeOfCurrentBomb = timeOfBombCanMoveOver,
                )
            },
            async(Dispatchers.Default) {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombCanMoveStrategy(dropBombLastTime),
                    isNearBomb = true,
                    noCheckTimeOfBomb = true,
                    timeOfCurrentBomb = timeOfBombCanMoveOver,
                )
            },
            async(Dispatchers.Default) {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombStrategy(),
                    isNearBomb = true,
                    noCheckTimeOfBomb = true,
                    timeOfCurrentBomb = timeOfBombCanMoveOver,
                    noCheckBomb = true,
                )
            }
        )
        val command = bestAndSafeCommand.ifEmpty {
            safeCommands
        }
//            //ln("Avoid BOMB direct" + safeCommands)
        if (command.isNotEmpty()) {
            sendCommand(command.firstOrNull(), gameInfo)
        } else {
            sendCommand(safeLast.firstOrNull(), gameInfo)
        }
    }

    private suspend fun getDirectionsDropBomb(gameInfo: GameInfo): List<Command> = coroutineScope {
        val allDirections = awaitAll(
            async {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = DropBombStrategy(dropBombLastTime, numberOfBalk = 3),
                )
            },
            async {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = DropBombStrategy(dropBombLastTime, numberOfBalk = 2),
                )
            },
            async {
                BotHandler.move(
                    position = gameInfo.player.currentPosition,
                    gameInfo = gameInfo,
                    targetPredicate = DropBombStrategy(dropBombLastTime),
                )
            }
        )

        val (direction3, direction2, anyDropBalkDirection) = allDirections
        println(
            """
            bomb3 = ${direction3.size}
            bomb2 = ${direction2.size}
            bombAny = ${anyDropBalkDirection.size}
        """.trimIndent()
        )
        when {
            direction3.size in 1..10 -> direction3
            direction2.size in 1..5 -> direction2
            else -> anyDropBalkDirection
        }
    }

    private fun sendCommand(command: Command?, gameInfo: GameInfo) {
        isMoving = false
        log.warning("COMMAND = $command")
//        if (command == Command.BOMB) {
//            bombs[gameInfo.player.currentPosition.row][gameInfo.player.currentPosition.col] = gameInfo.timestamp + 2200
//        }
        command ?: return
        botSocket?.emit(Event.DRIVE.value, Direction(command.value).toJson())
    }

    companion object {
        const val COMPLETE_EXPOSED_TIME = 700L
        val log = Logger.getLogger(BotExecutor::class.java.name)
        private const val CONTINUE_MOVE_CNT = 3
    }
}