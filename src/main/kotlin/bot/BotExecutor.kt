package bot

import bot.model.*
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
                onReceiveGame(gameInfo.copy(playerId = playerInfo.playerId))
            }
    }

    private suspend fun onReceiveGame(gameInfo: GameInfo) {
//        println("game info is $gameInfo")
//        println("game tag is ${gameInfo.tag}")
        when (gameInfo.tag) {
            GameTag.PLAYER_BACK_TO_PLAY -> {
//                targetPosition = player?.currentPosition
            }

            GameTag.START_GAME -> {
                targetPosition = gameInfo.player.currentPosition
            }

            GameTag.PLAYER_STOP_MOVING -> {
            }

            GameTag.UPDATE_DATA -> {
            }

            GameTag.BOMB_SETUP -> {
                println("BOM SETUP")
            }

            else -> {}
        }
        println("target position = $targetPosition, current position = ${gameInfo.player.currentPosition}")

//        if (gameInfo.timestamp - timestampLast > 3000) {
        startMove(gameInfo)
//        }

    }

    private var timestampLast = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    private suspend fun startMove(gameInfo: GameInfo) {
        isMoving = true
        val positions = botHandler.move(
            gameInfo = gameInfo,
            targetPredicate = { position ->
                getTargetPredicate(position, gameInfo)
            },
            onDeterminedTargetPos = {},
            canDropBomb = gameInfo.timestamp - dropBombLastTime <= (gameInfo.player.delay)
        )
        isMoving = false
        if (positions.isEmpty()) return
        targetPosition = positions.last().copy(command = null)
        val commandsResult = positions.mapNotNull(Position::command)

        println("Direction is ${commandsResult.toDirection().toJson()}")
//        currentDirection = commandsResult.toDirection()
//        botSocket?.emit(Event.DRIVE.value, listOf(Command.STOP).toDirection().toJson())
        botSocket?.emit(Event.DRIVE.value, Direction(commandsResult.first().value).toJson())
    }

    private suspend fun getCommandGotoSafeZone(gameInfo: GameInfo, bombPosition: Position): List<Position> {
        gameInfo.mapInfo.bombs.add(
            Bomb(
                col = bombPosition.col,
                row = bombPosition.row,
                playerId = gameInfo.playerId ?: "",
                remainTime = 0
            )
        )
        gameInfo.player.currentPosition = bombPosition.copy(command = null)
        return botHandler.move(
            canDropBomb = gameInfo.timestamp - dropBombLastTime <= (gameInfo.player.delay),
            gameInfo = gameInfo,
            targetPredicate = { position ->
                val isSafePosition = !gameInfo.checkIsNearBomb(position)
                println("Player is near bomb, isSafe $isSafePosition")
                TargetPredicate(
                    isTarget = isSafePosition
                )
            }
        ).ifEmpty { listOf(bombPosition) }
    }

    private var dropBombLastTime = 0L

    private fun sendCommand(command: Command) {
        botSocket?.emit(Event.DRIVE.value, Direction(command.value).toJson())
    }

    private fun getTargetPredicate(
        position: Position,
        gameInfo: GameInfo,
        isBeginPosition: Boolean = false
    ): TargetPredicate {
        println("getTargetPredicate current position ${position} - bomb = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb()) {
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
                spoilsNeedGet = listOf(
                    SpoilType.ATTACK_DRAGON_EGG,
                    SpoilType.SPEED_DRAGON_EGG,
                    SpoilType.DELAY_TIME_DRAGON_EGG
                )
            ) -> {
                println("checkSpoilNeedGet true")
                TargetPredicate(isTarget = true)
            }

            gameInfo.checkPositionIsNearBalk(
                position = position
            ) -> {
                println(" checkPositionIsNearBalk true, gameTimeStamp = ${gameInfo.timestamp}, dropbombLastime = $dropBombLastTime")
                (if (gameInfo.getBombRemainTimePlayer()>= 0) {
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
}