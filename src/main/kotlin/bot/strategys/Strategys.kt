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
        val isTarget =
            !checkPositionIsNearBomb(gameInfo, position = position, bombPosition = bombPosition) && isSafePosition
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
            isTarget = isSafePosition && isCanMove && gameInfo.checkPositionIsNearBalk(position) >0
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

/**
 * Check and return Bomb command if can drop bomb
 */
private fun getBombCommand(position: Position, gameInfo: GameInfo, dropBombLastTime: Long): Command? {
    val numberOfBalkAttacked = gameInfo.checkPositionIsNearBalk(
        position = position
    )
    val canDropBomb = gameInfo.timestamp - dropBombLastTime >= gameInfo.player.delay
    if (!canDropBomb) return null
    //ln("START check to drop bomb")
    val oldTimeStamp = gameInfo.bombs[position.row][position.col]
    if (numberOfBalkAttacked == 0) return null
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
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
        val numberOfBalkAttacked = gameInfo.checkPositionIsNearBalk(
            position = position
        )
        val canDropBomb = gameInfo.timestamp - dropBombLastTime >= gameInfo.player.delay
        if (!canDropBomb) return TargetPredicate()
        //ln("START check to drop bomb")
        val oldTimeStamp = gameInfo.bombs[position.row][position.col]
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

        return TargetPredicate(
            isTarget = canDropBomb,
            commandNeedPerformed = Command.BOMB.takeIf { isTarget && canMove })
    }

    companion object {
        const val DROP_ANY_BALK = Int.MAX_VALUE
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

class GetSpoilsStrategy(private val dropBombLastTime: Long) : StrategyMove {
    override fun predicate(position: Position, gameInfo: GameInfo): TargetPredicate {
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
        val bombCommand = getBombCommand(position = position, gameInfo = gameInfo, dropBombLastTime = dropBombLastTime)
        return TargetPredicate(isTarget = isPositionHaveSpoilNeedGet, commandNeedPerformed = bombCommand)
    }
}