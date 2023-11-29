package bot.strategys

import bot.BotHandler
import bot.TargetPredicate
import bot.model.*
import kotlin.math.abs

interface StrategyMove {
    fun predicate(position: Position, gameInfo: GameInfo, needDropBombWhenStartMove: Boolean = false): TargetPredicate
}

class MoveToTargetStrategy(private val target: Position) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        return TargetPredicate(
            isTarget = position.row == target.row && position.col == target.col
        )
    }
}

class KillMaxPowerStrategy(private val dropBombLastTime: Long, private val lengthOfBomb: Int = 0) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        val isCanKillPlayer = gameInfo.checkPositionIsNearCompetitor(
            position = position,
            lengthOfBomb = lengthOfBomb
        )
        if (!isCanKillPlayer) return TargetPredicate()
        val commandToSafe = BotHandler.move(
            position = position,
            gameInfo = gameInfo,
            targetPredicate = AvoidBombStrategy(position),
            isNearBomb = false,
            noCheckTimeOfBomb = true,
        )
        val commandCompetitorToSafe = BotHandler.move(
            position = gameInfo.competitor.currentPosition,
            gameInfo = gameInfo,
            targetPredicate = AvoidBombStrategy(position),
            isNearBomb = false,
            bomPosition = position,
            noCheckTimeOfBomb = true,
        )
        return if (gameInfo.timestamp - dropBombLastTime <= gameInfo.player.delay) {
            TargetPredicate()
        } else {
            TargetPredicate(
                isTarget = true,
                commandNeedPerformed = Command.BOMB.takeIf {
                    commandToSafe.isNotEmpty() &&
                            commandCompetitorToSafe.isEmpty()
                })
        }
    }
}

class AvoidBombStrategy(private val bombPosition: Position = Position.NONE) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        //ln("Player is near bomb position = $position, isSafe $isSafePosition")
        val isTarget =
            !checkPositionIsNearBomb(gameInfo, position = position, bombPosition = bombPosition) && isSafePosition
        return TargetPredicate(
            isTarget = isTarget
        )
    }
}

private fun checkPositionIsNearBomb(gameInfo: GameInfo, position: Position, bombPosition: Position): Boolean {
    if (position.row == bombPosition.row && abs(position.col - bombPosition.col) <= gameInfo.lengthOfBomb) return true
    return position.col == bombPosition.col && abs(position.row - bombPosition.row) <= gameInfo.lengthOfBomb
}

class AvoidBombCanMoveStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        val isSafePosition = gameInfo.checkPositionIsSafe(position)
        //ln("Player is near bomb position = $position, isSafe $isSafePosition")
//        val isCanMove = gameInfo.checkCanMoveSafe(position)
        val direction = BotHandler.move(
            position = position,
            gameInfo = gameInfo,
            targetPredicate = DropBombStrategy(dropBombLastTime)
        )
        return TargetPredicate(
            isTarget = isSafePosition && direction.isNotEmpty()
        )
    }
}

class AvoidBombAndGetSpoil : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
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

/**
 * Check and return Bomb command if can drop bomb
 */
private fun getBombCommand(
    position: Position,
    gameInfo: GameInfo,
    dropBombLastTime: Long,
    numberOfBalk: Int
): Command? {
    val numberOfBalkAttacked = gameInfo.checkPositionIsNearBalk(
        position = position
    )
    val canDropBomb = gameInfo.timestamp - dropBombLastTime >= gameInfo.player.delay
    if (!canDropBomb) return null
//    println("Number of balk = $numberOfBalkAttacked")
    //ln("START check to drop bomb")
//    val oldTimeStamp = gameInfo.bombs[position.row][position.col]
    if (numberOfBalkAttacked != numberOfBalk) return null
//          numberOfBalkAttacked  gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000
    val commandToSafe = BotHandler.move(
        position = position,
        gameInfo = gameInfo,
        targetPredicate = AvoidBombStrategy(position),
        isNearBomb = false,
        noCheckTimeOfBomb = true,
    )
    val canMove = gameInfo.checkCanMoveSafe(position)
    val isTarget =
        commandToSafe.isNotEmpty()

    return Command.BOMB.takeIf { isTarget && canMove }
}

class DropBombStrategy(private val dropBombLastTime: Long, private val numberOfBalk: Int = DROP_ANY_BALK) :
    StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        val numberOfBalkAttacked = gameInfo.checkPositionIsNearBalk(
            position = position
        )
        val canDropBomb = gameInfo.timestamp - dropBombLastTime >= gameInfo.player.delay
        if (!canDropBomb) return TargetPredicate()
        //ln("START check to drop bomb")
//        val oldTimeStamp = gameInfo.bombs[position.row][position.col]
        if (numberOfBalk != DROP_ANY_BALK && numberOfBalkAttacked != numberOfBalk) return TargetPredicate()
        if (numberOfBalkAttacked == 0) return TargetPredicate()
//          numberOfBalkAttacked  gameInfo.bombs[position.row][position.col] = gameInfo.timestamp + 2000
        val commandToSafe = BotHandler.move(
            position = position,
            gameInfo = gameInfo,
            targetPredicate = AvoidBombStrategy(position),
            isNearBomb = false,
            noCheckTimeOfBomb = true,
        )
        val canMove = gameInfo.checkCanMoveSafe(position)
        val isTarget =
            commandToSafe.isNotEmpty()
//        println("numberOfBalkAttacked = ${numberOfBalkAttacked>0}")

        return TargetPredicate(
            isTarget = canDropBomb,
            commandNeedPerformed = Command.BOMB.takeIf { isTarget && canMove })
    }

    companion object {
        const val DROP_ANY_BALK = Int.MAX_VALUE
    }
}

class AttackCompetitorEggStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
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

// Get spoil and drop bomb
class GetSpoilsStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        //ln("GEt GetSpoilsStrategy")
        val isPositionHaveSpoilNeedGet = gameInfo.checkSpoilNeedGet(
            position,
            spoilsNeedGet = listOf(
                SpoilType.ATTACK_DRAGON_EGG,
                SpoilType.SPEED_DRAGON_EGG,
                SpoilType.DELAY_TIME_DRAGON_EGG,
                SpoilType.MYSTIC_DRAGON_EGG
            )
        )
        val bombCommand = getBombCommand(
            position = position,
            gameInfo = gameInfo,
            dropBombLastTime = dropBombLastTime,
            numberOfBalk = 3
        )
//        println("Drop bomb getSpoid =" + bombCommand.takeIf { needDropBombWhenStartMove })
        return TargetPredicate(
            isTarget = isPositionHaveSpoilNeedGet,
            commandNeedPerformed = bombCommand.takeIf { needDropBombWhenStartMove })
    }
}

class KillCompetitorStrategy(private val dropBombLastTime: Long, private val lengthOfBomb: Int) : StrategyMove {
    override fun predicate(
        position: Position,
        gameInfo: GameInfo,
        needDropBombWhenStartMove: Boolean
    ): TargetPredicate {
        val isCanKillPlayer = gameInfo.checkPositionIsNearCompetitor(
            position = position,
            lengthOfBomb = lengthOfBomb
        )
        if (!isCanKillPlayer) return TargetPredicate()
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