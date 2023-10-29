package bot

import bot.model.*
import io.socket.client.Socket
import kotlinx.coroutines.delay
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

            GameTag.BOMB_SETUP -> {
                println("BOM SETUP")
            }

            else -> {}
        }
        startMove(gameInfo)
    }

    private var timestampLast = 0L

    private var targetPosition: Position? = null
    private var isMoving: Boolean = false

    class AvoidBombStrategy : StrategyMove {
        override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
            val isSafePosition = !gameInfo.checkIsNearBomb(position)
            println("Player is near bomb position = $position, isSafe $isSafePosition")
            return TargetPredicate(
                isTarget = isSafePosition
            )
        }
    }

    class AvoidBombAndGetSpoil : StrategyMove {
        override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
            val isSafePosition = !gameInfo.checkIsNearBomb(position) && gameInfo.checkSpoilNeedGet(
                position,
                spoilsNeedGet = listOf(
                    SpoilType.ATTACK_DRAGON_EGG,
                    SpoilType.SPEED_DRAGON_EGG,
                    SpoilType.DELAY_TIME_DRAGON_EGG
                )
            )
            println("Player is near bomb, isSafe $isSafePosition")
            return TargetPredicate(
                isTarget = isSafePosition
            )
        }
    }

    class DropBombStrategy : StrategyMove {
        override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
            val isPositionNeedDropBomb = gameInfo.checkPositionIsNearBalk(
                position = position
            )
            if (!isPositionNeedDropBomb) return TargetPredicate()
            return if (gameInfo.getBombRemainTimePlayer() >= 0) {
                TargetPredicate()
            } else {
                TargetPredicate(isTarget = true, commandNeedPerformed = Command.BOMB)
            }
        }
    }

    class AttackCompetitorEggStrategy : StrategyMove {
        override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
            val isPositionNeedAttack = gameInfo.checkPositionIsNearCompetitorEgg(
                position = position
            )
            if (!isPositionNeedAttack) return TargetPredicate()
            return if (gameInfo.getBombRemainTimePlayer() >= 0) {
                TargetPredicate()
            } else {
                TargetPredicate(isTarget = true, commandNeedPerformed = Command.BOMB)
            }
        }
    }

    class GetSpoilsStrategy : StrategyMove {
        override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
            val isPositionHaveSpoilNeedGet = gameInfo.checkSpoilNeedGet(
                position,
                spoilsNeedGet = listOf(
                    SpoilType.ATTACK_DRAGON_EGG,
                    SpoilType.SPEED_DRAGON_EGG,
                    SpoilType.DELAY_TIME_DRAGON_EGG
                )
            )
            return TargetPredicate(isTarget = isPositionHaveSpoilNeedGet)
        }
    }

    private val dropBombStrategy = DropBombStrategy()

    interface StrategyMove {
        fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate
    }

    private suspend fun startMove(gameInfo: GameInfo) {
        // Check if player is dangerous, and move to safe zone.
        println("player = ${gameInfo.player}, currentPosition = ${gameInfo.player.currentPosition}, boms = ${gameInfo.mapInfo.bombs}")
        if (gameInfo.checkIsNearBomb()) {
            val safeCommands =
                botHandler.move(gameInfo = gameInfo, targetPredicate = AvoidBombStrategy(), isNearBomb = true)
            val bestAndSafeCommand =
                botHandler.move(gameInfo = gameInfo, isNearBomb = true, targetPredicate = AvoidBombAndGetSpoil())
            val command = if (bestAndSafeCommand.isNotEmpty()) {
                bestAndSafeCommand.firstOrNull()
            } else {
                safeCommands.firstOrNull()
            }
            println("Avoid BOMB direct" + safeCommands)
            sendCommand(command)
            return
        }

//        // Drop bomb if player position can drop bomb.
//        if (dropBombStrategy.predicate(gameInfo = gameInfo, position = gameInfo.player.currentPosition).isTarget) {
//            println("PLACE BOMB")
//            sendCommand(Command.BOMB)
//            return
//        }

        // Move to an advantageous position
        val dropBombDirections = botHandler.move(gameInfo = gameInfo, targetPredicate = DropBombStrategy())
        val getSpoilDirections = botHandler.move(gameInfo = gameInfo, targetPredicate = GetSpoilsStrategy())
        println("DROP bomb = $dropBombDirections")
        println("GET spoil = $getSpoilDirections")

        // If can't get spoil and drop bomb, perform attack competitor egg.
        if (dropBombDirections.isEmpty() && getSpoilDirections.isEmpty()) {
            val directionsAttach = botHandler.move(gameInfo = gameInfo, targetPredicate = AttackCompetitorEggStrategy())
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
//        isMoving = false
//        if (positions.isEmpty()) return
//        targetPosition = positions.last().copy(command = null)
//        val commandsResult = positions.mapNotNull(Position::command)
//        println("Direction is ${commandsResult.toDirection().toJson()}")
//        botSocket?.emit(Event.DRIVE.value, Direction(commandsResult.first().value).toJson())
    }

    private var dropBombLastTime = 0L

    private fun sendCommand(command: Command?) {
        isMoving = false
        println("COMMAND = $command")
        command ?: return
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
                (if (gameInfo.getBombRemainTimePlayer() >= 0) {
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