package bot

import bot.model.*
import bot.strategys.StrategyMove
import java.util.*
import java.util.logging.Logger
import kotlin.collections.HashMap
import kotlin.math.abs

val dx = listOf(0, -1, 0, 1)
val dy = listOf(-1, 0, 1, 0)

data class TargetPredicate(
    val isTarget: Boolean = false, val commandNeedPerformed: Command? = null
)

object BotHandler {
//    abstract fun attack(command: List<Command>)

    fun move(
        position: Position,
        gameInfo: GameInfo,
        targetPredicate: StrategyMove,
        isNearBomb: Boolean = false,
        noCheckTimeOfBomb: Boolean = false,
        timeOfCurrentBomb: Long = 0,
    ): List<Command> {
//        val playerPosition = gameInfo.player.currentPosition
        val beginTargetPredicate = targetPredicate.predicate(position, gameInfo)
        if (beginTargetPredicate.commandNeedPerformed == Command.BOMB) return listOf(Command.BOMB)
        val competitorPosition = gameInfo.competitor.currentPosition
        val visits: List<MutableList<Boolean>> =
            List(gameInfo.mapInfo.size.rows) { MutableList(gameInfo.mapInfo.size.cols) { false } }
        val moveQueue: Queue<Position> = LinkedList()
        val paths: HashMap<Position?, Position?> = hashMapOf()
        visits[position.row][position.col] = true
        moveQueue.add(position)
        val playerIsFreeze = checkPlayerFreezeMove(gameInfo)
        //ln("Freeze = $playerIsFreeze, currentPosition = ${gameInfo.player.currentPosition}")
        // Stop when continue check next action
        while (moveQueue.isNotEmpty()) {
            val position = moveQueue.poll()
            // Position at bomb line, force move over only this bomb.
            val forceMoveOverBomb = isNearBomb
            for (i in dx.indices) {
                val nextPosition = Position(
                    row = position.row + dx[i],
                    col = position.col + dy[i],
                    command = getCommand(i)
                )
                val positionIsBombOfPlayer = checkPositionIsNearBomb(gameInfo, nextPosition, position)
                val moveOverBomb = (forceMoveOverBomb || playerIsFreeze) && gameInfo.checkForceMoveOverBomb(
                    position,
                    timeOfCurrentBomb
                )
                log.warning("moveOverBom=$moveOverBomb")
                if(isNearBomb && !moveOverBomb) continue
                if (gameInfo.checkPositionIsInbound(nextPosition) &&
                    !visits[nextPosition.row][nextPosition.col] &&
                    checkCanMove(
                        position = nextPosition,
                        competitorPosition = competitorPosition,
                        gameInfo = gameInfo,
                        forceMoveOverBomb = moveOverBomb || playerIsFreeze,
                        noCheckTimeOfBomb = noCheckTimeOfBomb
                    )
                ) {
                    visits[nextPosition.row][nextPosition.col] = true
                    paths[nextPosition] = position
                    moveQueue.add(nextPosition)
                    val targetPredicateRes = targetPredicate.predicate(nextPosition, gameInfo)
                    if (targetPredicateRes.isTarget) {
                        val positions: MutableList<Position> = getPositions(
                            paths = paths,
                            targetPos = nextPosition
                        ).toMutableList()
                        return positions.mapNotNull(Position::command).toMutableList().apply {
                            targetPredicateRes.commandNeedPerformed?.let { command ->
                                add(command)
                            }
                        }
                    }
                }
            }
        }
        return listOf()
    }

    private fun checkPlayerFreezeMove(gameInfo: GameInfo): Boolean {
        if (!gameInfo.checkIsNearBomb()) return false
        val currentPosition = gameInfo.player.currentPosition
        val leftPosition = Position(row = currentPosition.row, col = currentPosition.col - 1)
        val rightPosition = Position(row = currentPosition.row, col = currentPosition.col + 1)
        val upPosition = Position(row = currentPosition.row - 1, col = currentPosition.col)
        val downPosition = Position(row = currentPosition.row + 1, col = currentPosition.col)
        val bombAtPlayer =
            gameInfo.mapInfo.bombs.firstOrNull { it.row == currentPosition.row || it.col == currentPosition.col }
        if (bombAtPlayer?.row == currentPosition.row) {
            return !checkCanMove(
                position = upPosition,
                competitorPosition = gameInfo.competitor.currentPosition,
                gameInfo = gameInfo,
                forceMoveOverBomb = true,
            ) &&
                    !checkCanMove(
                        position = downPosition,
                        competitorPosition = gameInfo.competitor.currentPosition,
                        gameInfo = gameInfo,
                        forceMoveOverBomb = true,
                    )
        }
        if (bombAtPlayer?.col == currentPosition.col) {
            return !checkCanMove(
                position = leftPosition,
                competitorPosition = gameInfo.competitor.currentPosition,
                gameInfo = gameInfo.copy(),
                forceMoveOverBomb = true,
            ) &&
                    !checkCanMove(
                        position = rightPosition,
                        competitorPosition = gameInfo.competitor.currentPosition,
                        gameInfo = gameInfo,
                        forceMoveOverBomb = true,
                    )
        }
        return false
    }

    private fun getPositions(
        paths: Map<Position?, Position?>,
        targetPos: Position
    ): List<Position> {
        return buildList {
            var position: Position? = targetPos
            while (position != null) {
                position.let(::add)
                position = paths[position]
            }
        }.reversed()
    }

    // Check whether if the position can move in, or drop bombs to eat items
    private fun checkCanMove(
        position: Position,
        competitorPosition: Position,
        gameInfo: GameInfo,
        forceMoveOverBomb: Boolean = false,
        noCheckTimeOfBomb: Boolean = false,
    ): Boolean {
//        //ln("player = ${gameInfo.playerId}, forceMoveOverBombs = $forceMoveOverBomb, position = $position")
        if (!gameInfo.checkPositionIsInbound(position)) return false
        if (gameInfo.mapInfo.bombs.any { it.row == position.row && it.col == position.col }) return false
        val item = gameInfo.mapInfo.map[position.row][position.col]
        val spoilItem = gameInfo.mapInfo.spoils.firstOrNull { it.row == position.row && it.col == position.col }
        // TODO remove comment if can move over competitor.
        if (position.row == competitorPosition.row && position.col == competitorPosition.col) return false
//        //ln("player = ${gameInfo.playerId}, checkNearBomb")
        if (!forceMoveOverBomb && gameInfo.checkIsNearBomb(
                position = position,
                noCheckTime = noCheckTimeOfBomb,
            )
        ) return false
//        //ln("player = ${gameInfo.playerId}, check is Mystic egg")
        if (forceMoveOverBomb && spoilItem?.spoilType in notShouldMoveSpoils) return true
//        //ln("player = ${gameInfo.playerId}, check is not move, item = $item")
        return !listOf(
            ItemType.BALK,
            ItemType.WALL,
            ItemType.QUARANTINE_PLACE,
            ItemType.TELEPORT_GATE,
            ItemType.DRAGON_EGG_GST
        ).contains(item) && spoilItem?.spoilType !in notShouldMoveSpoils
    }

    private fun checkCanMove(position: Position, gameInfo: GameInfo): Boolean {
        val item = gameInfo.mapInfo.map[position.row][position.col]
        val spoilItem = gameInfo.mapInfo.spoils.firstOrNull { it.row == position.row && it.col == position.col }
        return !listOf(
            ItemType.BALK,
            ItemType.WALL,
            ItemType.QUARANTINE_PLACE,
            ItemType.TELEPORT_GATE,
            ItemType.DRAGON_EGG_GST
        ).contains(item) && spoilItem?.spoilType !in notShouldMoveSpoils
    }

    private fun checkCanMoveSafe(position: Position, gameInfo: GameInfo): Boolean {
        for (i in dx.indices) {
            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
            if (gameInfo.checkPositionIsSafe(nextPosition) && checkCanMove(nextPosition, gameInfo)) return true
        }
        return false
    }

    private fun checkPositionIsNearBomb(gameInfo: GameInfo, position: Position, bombPosition: Position): Boolean {
        if (position.row == bombPosition.row && abs(position.col - bombPosition.col) <= gameInfo.lengthOfBomb) return true
        return position.col == bombPosition.col && abs(position.row - bombPosition.row) <= gameInfo.lengthOfBomb
    }

    fun getCommand(dxyIndex: Int): Command {
        return when (dxyIndex) {
            0 -> Command.LEFT
            1 -> Command.UP
            2 -> Command.RIGHT
            else -> Command.DOWN
        }
    }

    private val log = Logger.getLogger(BotExecutor::class.java.name)
    private val notShouldMoveSpoils = listOf<SpoilType>()
}