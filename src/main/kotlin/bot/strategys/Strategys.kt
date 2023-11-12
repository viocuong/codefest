package bot.strategys

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

class AvoidBombCanMoveStrategy : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        println("Player is near bomb position = $position, isSafe $isSafePosition")
        val isCanMove = gameInfo.checkCanMoveSafe(position)
        return TargetPredicate(
            isTarget = isSafePosition && isCanMove
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

class DropBombStrategy(private val dropBombLastTime: Long, private val numberOfBalk: Int) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val numberOfBalkAttacked = gameInfo.checkPositionIsNearBalk(
            position = position
        )
        if (numberOfBalkAttacked != numberOfBalk) return TargetPredicate()
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
//            gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000
            val commandToSafe = BotHandler.move(
                gameInfo = gameInfo,
                targetPredicate = AvoidBombStrategy(),
                isNearBomb = false,
                noCheckTimeOfBomb = true,
            )
            val commandToBestSafe = BotHandler.move(
                gameInfo = gameInfo,
                targetPredicate = AvoidBombCanMoveStrategy(),
                isNearBomb = false,
                noCheckTimeOfBomb = true
            )
            val canMove = gameInfo.checkCanMoveSafe(position)
            println("START check to drop bomb result = $commandToSafe")
//            gameInfo.bombs[position.row][position.col] = oldTimeStamp
            val isTarget = if (gameInfo.player.currentPosition == position) {
                commandToSafe.isNotEmpty() || commandToBestSafe.isNotEmpty()
            } else {
                true
            }
            TargetPredicate(
                isTarget = commandToSafe.isNotEmpty() && canMove,
                commandNeedPerformed = Command.BOMB.takeIf { isTarget && canMove })
        }
    }
}

class AttackCompetitorEggStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isPositionNeedAttack = gameInfo.checkPositionIsNearCompetitorEgg(
            position = position
        )
        if (!isPositionNeedAttack) return TargetPredicate()
        return if (gameInfo.timestamp - dropBombLastTime <= gameInfo.player.delay) {
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