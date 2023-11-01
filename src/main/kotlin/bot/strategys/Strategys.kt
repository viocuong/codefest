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
            TargetPredicate()
        } else {
            val mapInfoHaveBomb = gameInfo.mapInfo.copy(
                bombs = gameInfo.mapInfo.bombs.toMutableList().apply {
                    Bomb(col = position.col, row = position.row, remainTime = 2000, playerId = gameInfo.playerId ?: "")
                }
            )
            val oldTimeStamp = gameInfo.bombs[position.row][position.col]
            gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000 + COMPLETE_EXPOSED_TIME
            val commandToSafe = BotHandler.move(
                gameInfo = gameInfo.copy(mapInfo = mapInfoHaveBomb),
                targetPredicate = AvoidBombStrategy(),
            )
            gameInfo.bombs[position.row][position.col] = oldTimeStamp
            TargetPredicate(isTarget = commandToSafe.isNotEmpty(), commandNeedPerformed = Command.BOMB)
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