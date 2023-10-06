package bot

import model.*
import java.util.*

val dx = listOf(0, -1, 0, 1)
val dy = listOf(-1, 0, 1, 0)

class BotHandler {
//    abstract fun attack(command: List<Command>)

    fun move(
        currentPosition: Position,
        mapInfo: MapInfo,
        targetPredicate: (position: Position) -> Pair<Boolean, Command> // First is predicate, second is command when position is target
    ): List<Command> {
        mapInfo.size ?: return emptyList()
        mapInfo.map ?: return emptyList()
        val visits: List<MutableList<Boolean>> = List(mapInfo.size.rows) { MutableList(mapInfo.size.cols) { false } }
        println("move visits=${visits}")
        val moveQueue: Queue<Position> = LinkedList()
        visits[currentPosition.row][currentPosition.col] = true
        moveQueue.add(currentPosition)
        val commands: MutableList<Command> = mutableListOf()
        // Stop when continue check next action
        commands.add(Command.STOP)
        while (moveQueue.isNotEmpty()) {
            val position = moveQueue.poll()
            for (i in 0 until 4) {
                val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
                val item = mapInfo.map[nextPosition.row][nextPosition.col]
                if (checkCanMove(position = nextPosition, mapSize = mapInfo.size, item = item)) {
                    visits[position.row][position.col] = true
                    commands.add(getCommand(i))
                    // Achieve target place
                    if (targetPredicate(position).first) {
                        commands.add(targetPredicate(position).second)
                        return commands
                    }
                }
            }
        }
        return commands
    }

    private fun checkPositionIsSpoil(mapInfo: MapInfo, spoilType: SpoilType?, position: Position): Boolean {
        return mapInfo.spoils?.any {
            it.row == position.row &&
                    it.col == position.col &&
                    it.spoilType == spoilType
        } ?: false
    }

    private fun checkCanMove(position: Position, mapSize: Size, item: ItemType): Boolean =
        position.row >= 0 && position.row < mapSize.rows && position.col >= 0 && position.col <= mapSize.cols && // Check Inbound map
                !listOf(ItemType.WALL, ItemType.BALK, ItemType.QUARANTINE_PLACE, ItemType.TELEPORT_GATE).contains(item)

    private fun getCommand(dxyIndex: Int): Command {
        return when (dxyIndex) {
            0 -> Command.LEFT
            1 -> Command.UP
            2 -> Command.RIGHT
            else -> Command.DOWN
        }
    }
}