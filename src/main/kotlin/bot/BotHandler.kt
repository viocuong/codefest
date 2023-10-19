package bot

import bot.model.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

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
    ): List<Position> {
        val playerPosition = gameInfo.player.currentPosition
        val competitorPosition = gameInfo.competitor.currentPosition
        val visits: List<MutableList<Boolean>> =
            List(gameInfo.mapInfo.size.rows) { MutableList(gameInfo.mapInfo.size.cols) { false } }
        val moveQueue: Queue<Position> = LinkedList()
        val paths: HashMap<Position?, Position?> = hashMapOf()
        visits[playerPosition.row][playerPosition.col] = true
        moveQueue.add(playerPosition)
        // Stop when continue check next action
        while (moveQueue.isNotEmpty()) {
            val position = moveQueue.poll()
            for (i in 0 until 4) {
                val nextPosition = Position(
                    row = position.row + dx[i],
                    col = position.col + dy[i],
                    command = getCommand(i)
                )
                if (checkIsInbound(nextPosition, gameInfo.mapInfo) && checkCanIsTarget(
                        position = nextPosition,
                        mapSize = gameInfo.mapInfo.size,
                        competitorPosition = competitorPosition,
                        mapInfo = gameInfo.mapInfo
                    ) && !visits[nextPosition.row][nextPosition.col]
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
                        // If not move then remove the command move before that
                        println("positions target is $positions, targetPredicate = $targetPredicateRes, current pos = ${gameInfo.mapInfo}")
                        onDeterminedTargetPos(positions.last())
                        return positions.toMutableList().apply {
                            targetPredicateRes.commandNeedPerformed?.let{command ->
                                add(last().copy(command = command))
                            }
                        }
                    }
                }
            }
        }
        return listOf()
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

    private fun checkPositionIsSpoil(mapInfo: MapInfo, spoilType: SpoilType?, position: Position): Boolean {
        return mapInfo.spoils?.any {
            it.row == position.row && it.col == position.col && it.spoilType == spoilType
        } ?: false
    }

    private fun checkIsInbound(position: Position, mapInfo: MapInfo): Boolean {
        return position.row >= 0 && position.row < mapInfo.size.rows && position.col >= 0 && position.col < mapInfo.size.cols
    }

    // Check whether if the position can move in, or drop bombs to eat items
    private fun checkCanIsTarget(
        position: Position,
        mapSize: Size,
        competitorPosition: Position,
        mapInfo: MapInfo
    ): Boolean {
        val item = mapInfo.map[position.row][position.col]
        if (position.row == competitorPosition.row && position.col == competitorPosition.col) return false
//        if (checkIsNearBomb(position, mapInfo.bombs)) return false
        return !listOf(
            ItemType.BALK,
            ItemType.WALL,
            ItemType.QUARANTINE_PLACE,
            ItemType.TELEPORT_GATE,
            ItemType.DRAGON_EGG_GST
        ).contains(item)
    }

    private fun checkIsNearBomb(position: Position, bombs: List<Bomb>): Boolean {
        val distancePlayerToBomb = 1
        return bombs.any { bomb ->
            (position.row == bomb.row && abs(position.col - bomb.col) <= distancePlayerToBomb) ||
                    (position.col == bomb.col && abs(position.row - bomb.row) <= distancePlayerToBomb)
        }
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