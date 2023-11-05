package bot.strategys

import bot.BotExecutor.Companion.COMPLETE_EXPOSED_TIME
import bot.BotHandler
import bot.TargetPredicate
import bot.model.*

interface StrategyMove {
    fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate
}

class AvoidBombStrategy : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        println("Player is near bomb position = $position, isSafe $isSafePosition")
        return TargetPredicate(
            isTarget = isSafePosition
        )
    }
}

class AvoidBombAndGetSpoil : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        println("AvoidBombAndGetSpoil player = ${gameInfo.playerId} position = $position")
        val isSafePosition = gameInfo.checkPositionIsSafe(position) && gameInfo.checkSpoilNeedGet(
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

class DropBombStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isPositionNeedDropBomb = gameInfo.checkPositionIsNearBalk(
            position = position
        )
        if (!isPositionNeedDropBomb) return TargetPredicate()
        return if (gameInfo.timestamp - dropBombLastTime < gameInfo.player.delay) {
            println("START check to drop bomb not enough time = ${gameInfo.timestamp - dropBombLastTime < gameInfo.player.delay}")
            TargetPredicate()
        } else {
            val mapInfoHaveBomb = gameInfo.mapInfo.copy(
                bombs = gameInfo.mapInfo.bombs.toMutableList().apply {
                    Bomb(col = position.col, row = position.row, remainTime = 2000, playerId = gameInfo.playerId ?: "")
                }
            )
            println("START check to drop bomb")
            val oldTimeStamp = gameInfo.bombs[position.row][position.col]
            //gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000
            val commandToSafe = BotHandler.move(
                gameInfo = gameInfo,
                targetPredicate = AvoidBombStrategy(),
                isNearBomb = false,
                noCheckTimeOfBomb = true
            )
            println("START check to drop bomb result = $commandToSafe")
            //gameInfo.bombs[position.row][position.col] = oldTimeStamp
            val isTarget = if (gameInfo.player.currentPosition == position) {
                commandToSafe.isNotEmpty()
            } else {
                true
            }
            TargetPredicate(
                isTarget = (gameInfo.player.currentPosition == position && commandToSafe.isNotEmpty()) || (commandToSafe.isNotEmpty() && gameInfo.player.currentPosition != position),
                commandNeedPerformed = Command.BOMB.takeIf { isTarget })
        }
    }
}

class AttackCompetitorEggStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isPositionNeedAttack = gameInfo.checkPositionIsNearCompetitorEgg(
            position = position
        )
        if (!isPositionNeedAttack) return TargetPredicate()
        return if (gameInfo.timestamp - dropBombLastTime < gameInfo.player.delay) {
            TargetPredicate()
        } else {
            TargetPredicate(isTarget = true, commandNeedPerformed = Command.BOMB)
        }
    }
}

class GetSpoilsStrategy : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        println("GEt GetSpoilsStrategy")
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