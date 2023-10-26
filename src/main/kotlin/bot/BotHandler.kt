package bot

import bot.model.*
import java.util.*
import kotlin.collections.HashMap

val dx = listOf(0, -1, 0, 1)
val dy = listOf(-1, 0, 1, 0)

data class TargetPredicate(
    val isTarget: Boolean = false, val commandNeedPerformed: Command? = null
)

class BotHandler {
//    abstract fun attack(command: List<Command>)

    suspend fun move(
        gameInfo: GameInfo,
        targetPredicate: suspend (position: Position) -> TargetPredicate,
        onDeterminedTargetPos: suspend (Position) -> Unit = {},
        canDropBomb: Boolean,
    ): List<Position> {
        val playerPosition = gameInfo.player.currentPosition
        val beginTargetPredicate = targetPredicate(playerPosition)
        if (beginTargetPredicate.commandNeedPerformed == Command.BOMB) return listOf(playerPosition.copy(command = Command.BOMB))
        val competitorPosition = gameInfo.competitor.currentPosition
        val visits: List<MutableList<Boolean>> =
            List(gameInfo.mapInfo.size.rows) { MutableList(gameInfo.mapInfo.size.cols) { false } }
        val moveQueue: Queue<Position> = LinkedList()
        val paths: HashMap<Position?, Position?> = hashMapOf()
        visits[playerPosition.row][playerPosition.col] = true
        moveQueue.add(playerPosition)
        //val playerIsFreeze = checkPlayerFreezeMove(gameInfo)
        // Stop when continue check next action
        while (moveQueue.isNotEmpty()) {
            val position = moveQueue.poll()
            for (i in 0 until 4) {
                val nextPosition = Position(
                    row = position.row + dx[i],
                    col = position.col + dy[i],
                    command = getCommand(i)
                )
                if (gameInfo.checkPositionIsInbound(nextPosition) &&
                    !visits[nextPosition.row][nextPosition.col] &&
                    checkCanMove(
                        position = nextPosition,
                        competitorPosition = competitorPosition,
                        gameInfo = gameInfo,
                        forceMoveOverBomb = gameInfo.checkPlayerAtBombPos()
                    )
                ) {
                    visits[nextPosition.row][nextPosition.col] = true
                    paths[nextPosition] = position
                    moveQueue.add(nextPosition)
                    // Achieve target place
                    val targetPredicateRes = targetPredicate(nextPosition)
//                    println("positions target targetPredicate = $targetPredicateRes")
                    if (targetPredicateRes.isTarget) {
                        val positions: MutableList<Position> = getPositions(
                            paths = paths,
                            targetPos = nextPosition
                        ).toMutableList()
                        onDeterminedTargetPos(positions.last())
                        return positions.toMutableList().apply {
                            targetPredicateRes.commandNeedPerformed?.let { command ->
                                add(last().copy(command = command))
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
            gameInfo.mapInfo.bombs.first { it.row == currentPosition.row || it.col == currentPosition.col }
        if (bombAtPlayer.row == currentPosition.row) {
            return !checkCanMove(
                position = leftPosition,
                competitorPosition = gameInfo.competitor.currentPosition,
                gameInfo = gameInfo,
                forceMoveOverBomb = true,
            ) &&
                    !checkCanMove(
                        position = rightPosition,
                        competitorPosition = gameInfo.competitor.currentPosition,
                        gameInfo = gameInfo,
                        forceMoveOverBomb = true,
                    )
        }
        if (bombAtPlayer.col == currentPosition.col) {
            return !checkCanMove(
                position = upPosition,
                competitorPosition = gameInfo.competitor.currentPosition,
                gameInfo = gameInfo.copy(),
                forceMoveOverBomb = true,
            ) &&
                    !checkCanMove(
                        position = downPosition,
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
        forceMoveOverBomb: Boolean = false
    ): Boolean {
        val item = gameInfo.mapInfo.map[position.row][position.col]
        val spoilItem = gameInfo.mapInfo.spoils.firstOrNull { it.row == position.row && it.col == position.col }
        if (position.row == competitorPosition.row && position.col == competitorPosition.col) return false
        if (!forceMoveOverBomb && gameInfo.checkIsNearBomb(position)) return false
        return !listOf(
            ItemType.BALK,
            ItemType.WALL,
            ItemType.QUARANTINE_PLACE,
            ItemType.TELEPORT_GATE,
            ItemType.DRAGON_EGG_GST
        ).contains(item) && spoilItem?.spoilType != SpoilType.MYSTIC_DRAGON_EGG
    }

    private fun getCommand(dxyIndex: Int): Command {
        return when (dxyIndex) {
            0 -> Command.LEFT
            1 -> Command.UP
            2 -> Command.RIGHT
            else -> Command.DOWN
        }
    }
}