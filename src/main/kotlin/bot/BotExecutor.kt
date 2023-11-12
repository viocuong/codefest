package bot

import bot.model.*
import bot.strategys.*
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val bombManager = BombManager()
    private var allBomb = mutableListOf<Bomb>()
    private var bombs: MutableList<MutableList<Long>> = mutableListOf()
    private var playerId: String = ""
    suspend fun initGame(host: String, clientInfo: PlayerInfo) {
        BotSocket.initSocket(host = host) {
            botSocket = this
            on(Event.JOIN_GAME.value) { args ->
                val gameInfo = JsonConverter.fromJson<PlayerInfo>(args[0] as JSONObject)
                if (clientInfo.playerId.contains(gameInfo?.playerId ?: "")) {
                    playerId = gameInfo?.playerId ?: ""
                }
//                println("Join game $data")
            }
            on(Socket.EVENT_CONNECT) {
                emit(
                    Event.JOIN_GAME.value, clientInfo.toJson()
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
                        playerId = playerId,
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
        println("GAME TAG = ${gameInfo.tag}")
        when (gameInfo.tag) {
            GameTag.PLAYER_BACK_TO_PLAY -> {
                if (gameInfo.isActionOfPlayer) {
                    startMove(gameInfo)
                }
            }

            GameTag.START_GAME -> {
                targetPosition = gameInfo.player.currentPosition
                if (gameInfo.isActionOfPlayer) {
                    startMove(gameInfo)
                }
            }

            GameTag.PLAYER_STOP_MOVING -> {
                if (gameInfo.isActionOfPlayer) {
                    startMove(gameInfo)
                }
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
                    val exposedEndTime = bomb.remainTime + gameInfo.timestamp
                    bombs[bomb.row][bomb.col] = maxOf(bombs[bomb.row][bomb.col], exposedEndTime)
                }
                println("BOM SETUP")
            }

            else -> {}
        }
        if (gameInfo.timestamp - lastTime > 50) {
            lastTime = gameInfo.timestamp
            startMove(gameInfo)
        }
    }

    private var lastTime = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    private suspend fun startMove(gameInfo: GameInfo) {
        // Check if player is dangerous, and move to safe zone.
        println("player = ${gameInfo.player}, currentPosition = ${gameInfo.player.currentPosition}, boms = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb(noCheckTime = true)) {
            val timeOfBomb = gameInfo.bombs[gameInfo.player.currentPosition.row][gameInfo.player.currentPosition.col]
            val safeCommands =
                BotHandler.move(
                    gameInfo = gameInfo,
                    targetPredicate = AvoidBombStrategy(),
                    isNearBomb = true,
                    noCheckTimeOfBomb = true,
                    timeOfCurrentBomb = timeOfBomb
                )
            val bestAndSafeCommand =
                BotHandler.move(
                    gameInfo = gameInfo,
                    isNearBomb = true,
                    noCheckTimeOfBomb = true,
                    targetPredicate = AvoidBombCanMoveStrategy(),
                    timeOfCurrentBomb = timeOfBomb,
                )
            println(
                """
                bestAndSafeCommand = $bestAndSafeCommand
                safeCommands = $safeCommands
            """.trimIndent()
            )
            val command = bestAndSafeCommand.ifEmpty {
                safeCommands
            }
            println("Avoid BOMB direct" + safeCommands)
            sendCommand(command.firstOrNull(), gameInfo)
            return
        }

        // Move to an advantageous position
        val (direction3, direction2, direction1) = withContext(Dispatchers.Default) {
            val dropBombDirections3 =
                async {
                    BotHandler.move(
                        gameInfo = gameInfo,
                        targetPredicate = DropBombStrategy(dropBombLastTime, numberOfBalk = 3),
                    )
                }
            val dropBombDirections2 =
                async {
                    BotHandler.move(
                        gameInfo = gameInfo,
                        targetPredicate = DropBombStrategy(dropBombLastTime, numberOfBalk = 2),
                    )
                }
            val dropBombDirections1 =
                async {
                    BotHandler.move(
                        gameInfo = gameInfo,
                        targetPredicate = DropBombStrategy(dropBombLastTime, numberOfBalk = 1),
                    )
                }
            awaitAll(dropBombDirections3, dropBombDirections2, dropBombDirections1)
        }
        val dropBombDirections = when {
            direction3.isNotEmpty() -> {
                direction3
            }

            direction2.isNotEmpty() -> {
                direction2
            }

            else -> {
                direction1
            }
        }
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
            sendCommand(directionsAttach.firstOrNull(), gameInfo)
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
        sendCommand(command, gameInfo)
    }

    private fun sendCommand(command: Command?, gameInfo: GameInfo) {
        isMoving = false
        println("COMMAND = $command")
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