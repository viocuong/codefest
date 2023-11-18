package bot.strategys

import bot.BotHandler
import bot.TargetPredicate
import bot.model.*
import kotlin.math.abs

interface StrategyMove {
    fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate
}

class AvoidBombStrategy(private val bombPosition: Position = Position.NONE) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        //ln("Player is near bomb position = $position, isSafe $isSafePosition")
        val isTarget = !checkPositionIsNearBomb(gameInfo, position = position, bombPosition = bombPosition) && isSafePosition
        return TargetPredicate(
            isTarget = isTarget
        )
    }

    private fun checkPositionIsNearBomb(gameInfo: GameInfo, position: Position, bombPosition: Position): Boolean {
        if (position.row == bombPosition.row && abs(position.col - bombPosition.col) <= gameInfo.lengthOfBomb) return true
        return position.col == bombPosition.col && abs(position.row - bombPosition.row) <= gameInfo.lengthOfBomb
    }
}

class AvoidBombCanMoveStrategy : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        //ln("Player is near bomb position = $position, isSafe $isSafePosition")
        val isCanMove = gameInfo.checkCanMoveSafe(position)
        return TargetPredicate(
            isTarget = isSafePosition && isCanMove
        )
    }
}

class AvoidBombAndGetSpoil : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        //ln("AvoidBombAndGetSpoil player = ${gameInfo.playerId} position = $position")
        val isSafePosition = gameInfo.checkPositionIsSafe(position) && gameInfo.checkSpoilNeedGet(
            position,
            spoilsNeedGet = listOf(
                SpoilType.ATTACK_DRAGON_EGG,
                SpoilType.SPEED_DRAGON_EGG,
                SpoilType.DELAY_TIME_DRAGON_EGG
            )
        )
        //ln("Player is near bomb, isSafe $isSafePosition")
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
        val canDropBomb = gameInfo.timestamp - dropBombLastTime >= gameInfo.player.delay
        if(!canDropBomb) return TargetPredicate()
        val mapInfoHaveBomb = gameInfo.mapInfo.copy(
            bombs = gameInfo.mapInfo.bombs.toMutableList().apply {
                Bomb(col = position.col, row = position.row, remainTime = 2000, playerId = gameInfo.playerId ?: "")
            }
        )
        //ln("START check to drop bomb")
        val oldTimeStamp = gameInfo.bombs[position.row][position.col]
//            gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000
        val commandToSafe = BotHandler.move(
            position = position,
            gameInfo = gameInfo,
            targetPredicate = AvoidBombStrategy(position),
            isNearBomb = false,
            noCheckTimeOfBomb = true,
        )
//        val commandToBestSafe = BotHandler.move(
//            gameInfo = gameInfo,
//            targetPredicate = AvoidBombCanMoveStrategy(),
//            isNearBomb = false,
//            noCheckTimeOfBomb = true
//        )
        val canMove = gameInfo.checkCanMoveSafe(position)
        //ln("START check to drop bomb result = $commandToSafe")
//            gameInfo.bombs[position.row][position.col] = oldTimeStamp
        val isTarget =
            commandToSafe.isNotEmpty()

        return TargetPredicate(
            isTarget = canDropBomb,
            commandNeedPerformed = Command.BOMB.takeIf { isTarget && canMove })
    }
}

class AttackCompetitorEggStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val isPositionNeedAttack = gameInfo.checkPositionIsNearCompetitorEgg(
            position = position
        )
        if (!isPositionNeedAttack) return TargetPredicate()
        val commandToSafe = BotHandler.move(
            position = position,
            gameInfo = gameInfo,
            targetPredicate = AvoidBombStrategy(position),
            isNearBomb = false,
            noCheckTimeOfBomb = true,
        )
        return if (gameInfo.timestamp - dropBombLastTime <= gameInfo.player.delay) {
            TargetPredicate()
        } else {
            TargetPredicate(isTarget = true, commandNeedPerformed = Command.BOMB.takeIf { commandToSafe.isNotEmpty() })
        }
    }
}

class GetSpoilsStrategy : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        //ln("GEt GetSpoilsStrategy")
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