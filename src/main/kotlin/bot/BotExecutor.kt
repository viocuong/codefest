package bot

import bot.model.*
import io.socket.client.Socket
import kotlinx.coroutines.*
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
    private var botHandler: BotHandler = BotHandler()
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
                //println("GAME TAG = ${gameInfo.tag}, currentPosition = ${player?.currentPosition}")
                onReceiveGame(gameInfo.copy(playerId = playerInfo.playerId))
            }
    }

    private var retryCount = 0
    private suspend fun onReceiveGame(gameInfo: GameInfo) {
        println("game info is $gameInfo")
        println("game tag is ${gameInfo.tag}")
        when (gameInfo.tag) {
            GameTag.PLAYER_BACK_TO_PLAY -> {
//                targetPosition = player?.currentPosition
            }

            GameTag.START_GAME -> {
                targetPosition = gameInfo.player.currentPosition
//                println(gameInfo)
//                println("START GAME")
//                 if(!isMoving){
//                     startMove()
//                 }
//                println("Bomb remain ${gameInfo.}")
//                botSocket?.emit(Event.DRIVE.value, listOf(Command.DOWN, Command.BOMB).toDirection().toJson())
//                botSocket?.emit(Event.DRIVE.value, listOf(Command.DOWN, Command.BOMB).toDirection().toJson())
//                botSocket?.emit(Event.DRIVE.value, listOf(Command.DOWN, Command.BOMB).toDirection().toJson())
            }

            GameTag.PLAYER_STOP_MOVING -> {
            }

            GameTag.UPDATE_DATA -> {
                //println("game info is: $gameInfo")
//                if (player?.currentPosition == targetPosition) {
//                startMove()
//                }
                //startMove()
            }

            GameTag.BOMB_SETUP -> {
                println("BOM SETUP")
                dropBombLastTime = gameInfo.timestamp
            }
//
//            GameTag.PLAYER_STOP_MOVING -> {
//                startMove()
//            }

            else -> {}
        }
        println(
            """
            target position = ${targetPosition}
            current position = ${gameInfo.player}
            isMoving = ${isMoving}
        """.trimIndent()
        )
        if (gameInfo.timestamp - timestampLast > 300) {
            timestampLast = gameInfo.timestamp
            println("timestamp = ${timestampLast}")
            startMove(gameInfo)
        }
    }

    private var timestampLast = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    private suspend fun startMove(gameInfo: GameInfo) {
        isMoving = true
//        println("game info is xxx ${gameInfo}")
        val currentPosition = gameInfo.player!!.currentPosition
        val competitorPosition = gameInfo.competitor!!.currentPosition
        var lastPosition = Position(0, 0)
        val positions = botHandler.move(
            gameInfo = gameInfo,
            targetPredicate = { position ->
                getTargetPredicate(position, gameInfo)
            },
            onDeterminedTargetPos = { targetPosition ->
                println(
                    "ON receive last position $targetPosition | item = ${
                        gameInfo.mapInfo.map[targetPosition.row][targetPosition.col]
                    }"
                )
                lastPosition = targetPosition
            }
        )
//        println("Last position of player = $lastPosition")
        isMoving = false
        if (positions.isEmpty()) return
        targetPosition = positions.last().copy(command = null)
        val commandsResult = if (positions.last().command == Command.BOMB) {
            // Move to the safe zone
            val commandsGoToSafeZone = getCommandGotoSafeZone(gameInfo = gameInfo, bombPosition = positions.last())
            println("lastPosition = ${positions.last()}, last target position = ${commandsGoToSafeZone.last()}")
            targetPosition = commandsGoToSafeZone.last().copy(command = null)
            positions.mapNotNull(Position::command).toMutableList()
                .apply { addAll(commandsGoToSafeZone.mapNotNull(Position::command)) }
        } else {
            positions.mapNotNull(Position::command)
        }

        println("Direction is ${commandsResult.toDirection().toJson()}")
        botSocket?.emit(Event.DRIVE.value, commandsResult.toDirection().toJson())
    }

    private suspend fun getCommandGotoSafeZone(gameInfo: GameInfo, bombPosition: Position): List<Position> {
        gameInfo.mapInfo.bombs.add(Bomb(
            col = bombPosition.col,
            row = bombPosition.row,
            playerId = gameInfo.playerId ?: "",
            remainTime = 0
        ))
        gameInfo.player.currentPosition = bombPosition
        return botHandler.move(
            gameInfo = gameInfo,
            targetPredicate = { _ ->
                TargetPredicate(isTarget = true)
            }
        ).ifEmpty { listOf(bombPosition) }
    }

    private var dropBombLastTime = 0L

    private fun getTargetPredicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        println("getTargetPredicate current position ${gameInfo.player!!.currentPosition} - bomb = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb(gameInfo.player.currentPosition)) {
            val isSafePosition = !gameInfo.checkIsNearBomb(position)
            println("Player is near bomb, isSafe $isSafePosition")
            return TargetPredicate(
                isTarget = isSafePosition
            )
        }
//        println("startMove position = $position")
        return when {
            gameInfo.checkSpoilNeedGet(
                position,
                spoilsNeedGet = listOf(SpoilType.ATTACK_DRAGON_EGG, SpoilType.SPEED_DRAGON_EGG)
            ) -> {
                println("checkSpoilNeedGet true")
                TargetPredicate(isTarget = true)
            }

            gameInfo.checkPositionIsNearBalk(
                position = position
            ) -> {
                println(" checkPositionIsNearBalk true, gameTimeStamp = ${gameInfo.timestamp}, dropbombLastime = $dropBombLastTime")
                (if (gameInfo.timestamp - dropBombLastTime <= (gameInfo.player!!.delay)
                ) {
                    TargetPredicate()
                } else {
                    TargetPredicate(isTarget = true, commandNeedPerformed = Command.BOMB)
                })
            }

            else -> {
                println("targetPredicate default")
                TargetPredicate()
            }
        }
    }

    companion object {
        private const val DELAY_INTERVAL = 500L
        private val adapterCustoms = listOf(GameTagAdapter(), ItemAdapter(), SpoilTypeAdapter())
    }
}