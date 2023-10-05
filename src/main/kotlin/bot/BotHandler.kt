package bot

import model.Command
import model.ItemType
import model.Position
import model.Size
import java.util.*
val dx = listOf(0, -1, 0, 1)
val dy = listOf(-1, 0, 1, 0)

class BotHandler {
//    abstract fun attack(command: List<Command>)

    fun move(
        map: List<List<ItemType>>,
        currentPosition: Position,
        targetItem: ItemType,
        mapSize: Size,
        commandAfterToTarget: Command,
    ): List<Command> {
        val visits: List<MutableList<Boolean>> = List(map.size) { MutableList(map[0].size) { false } }
        val moveQueue: Queue<Position> = LinkedList()
        visits[currentPosition.row][currentPosition.row] = true
        moveQueue.add(currentPosition)
        val commands: MutableList<Command> = mutableListOf()
        // Stop when continue check next action
        commands.add(Command.STOP)

        while (moveQueue.isNotEmpty()) {
            val position = moveQueue.poll()
            for (i in 0 until 4) {
                val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
                val item = map[nextPosition.col][nextPosition.col]
                if (checkCanMove(position = nextPosition, mapSize = mapSize, item = item)) {
                    visits[position.row][position.col] = true
                    commands.add(getCommand(i))
                    // Achieve target place
                    if (map[nextPosition.row][nextPosition.col] == targetItem) {
                        // Perform action when has arrived at the target place [drop bomb or stop]
                        commands.add(commandAfterToTarget)
                        return commands
                    }
                }
            }
        }
        return commands
    }

    private fun checkCanMove(position: Position, mapSize: Size, item: ItemType): Boolean =
        position.row >= 0 && position.row < mapSize.rows && position.col >= 0 && position.col <= mapSize.cols && // Check Inbound map
                !listOf(ItemType.WALL, ItemType.BALK, ItemType.QUARANTINE_PLACE).contains(item)

    private fun getCommand(dxyIndex: Int): Command {
        return when (dxyIndex) {
            0 -> Command.LEFT
            1 -> Command.UP
            2 -> Command.RIGHT
            else -> Command.DOWN
        }
    }
}