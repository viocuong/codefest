package bot.model

import bot.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import utils.JsonConverter.toJson
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@JsonClass(generateAdapter = true)
data class GameInfo(
    val gameRemainTime: Int,
    val id: Int,
    @Json(name = "map_info") val mapInfo: MapInfo,
    val tag: GameTag?,
    val timestamp: Long,
    @Json(name = "player_id") val playerIdOfAction: String?,
    @Transient
    val playerId: String? = "",
    @Transient
    val bombManager: BombManager? = null,
    @Transient
    val bombsExposing: List<Bomb> = emptyList(),
    @Transient
    val bombs: List<MutableList<Long>> = emptyList()
) {
    val player: Player get() = mapInfo.players.first { it.id == playerId }
    val competitor: Player get() = mapInfo.players.first { it.id != playerId }

    val isActionOfPlayer: Boolean get() = playerIdOfAction == playerId

    fun checkPositionIsNearCompetitorEgg(position: Position): Boolean {
        if (playerId == "player2-xxx") {
            //ln("checkPositionIsNearCompetitorEgg $position")
        }
        val competitorEgg = mapInfo.dragonEggGSTArray.firstOrNull { it.id != playerId } ?: return false
        if (playerId == "player2-xxx") {
            //ln("checkPositionIsNearCompetitorEgg egg =  $competitorEgg, lengBomb = $lengthOfBomb")
        }
        val canVerticalAttack =
            (position.col == competitorEgg.col && abs(position.row - competitorEgg.row) <= lengthOfBomb)
        val canHorizontalAttack =
            (position.row == competitorEgg.row && abs(position.col - competitorEgg.col) <= lengthOfBomb)
        var hasWallOnVerticalAttack = false
        var hasWallOnHorizontalAttack = false
        if (canVerticalAttack) {
            var index = min(position.row, competitorEgg.row)
            val lastIndex = max(position.row, competitorEgg.row)
            while (index++ < lastIndex) {
                if (mapInfo.map[index][position.col] == ItemType.WALL) {
                    hasWallOnVerticalAttack = true
                    break
                }
            }
        }
        if (canHorizontalAttack) {
            var index = min(position.col, competitorEgg.col)
            val lastIndex = max(position.col, competitorEgg.col)
            while (index++ < lastIndex) {
                if (mapInfo.map[position.row][index] == ItemType.WALL) {
                    hasWallOnHorizontalAttack = true
                    break
                }
            }
        }
        return (canVerticalAttack && !hasWallOnVerticalAttack) || (canHorizontalAttack && !hasWallOnHorizontalAttack)
//        for (i in 0 until 4) {
//            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
//            if (checkPositionInbound(nextPosition) && competitorEgg.row == nextPosition.row && competitorEgg.col == nextPosition.col) {
//                return true
//            }
//        }
//        return false
    }

    fun checkPositionIsNearBalk(position: Position): Int {
        log.warning("checkPositionIsNearBalk = $position")
        val itemsBombNotAttackOver = listOf(ItemType.WALL)
        var index = 0
        val numberOfBalkAttacked = 0
        var balkLeft = 0
        var balkUp = 0
        var balkRight = 0
        var balkDown = 0
        while (index <= lengthOfBomb) {
            for (i in dx.indices) {
                val x = position.row + dx[i] * index
                val y = position.col + dy[i] * index
                if (x >= 0 && x < mapInfo.size.rows && y >= 0 && y < mapInfo.size.cols) {
                    val nextPosition = Position(row = x, col = y)
                    if (!checkPositionInbound(nextPosition)) continue
                    val isNotAttack = getItem(nextPosition) in itemsBombNotAttackOver
                    val isItemBalk = getItem(nextPosition) == ItemType.BALK
                    when (BotHandler.getCommand(i)) {
                        Command.LEFT -> {
                            if (balkLeft != NO_ATTACK_BALK && isNotAttack) {
                                balkLeft = NO_ATTACK_BALK
                            } else if (isItemBalk && balkLeft != NO_ATTACK_BALK) {
                                balkLeft = 1
                            }
                        }

                        Command.UP -> {
                            if (balkUp != NO_ATTACK_BALK && isNotAttack) {
                                balkUp = NO_ATTACK_BALK
                            } else if (isItemBalk && balkUp != NO_ATTACK_BALK) {
                                balkUp = 1
                            }
                        }

                        Command.RIGHT -> {
                            if (balkRight != NO_ATTACK_BALK && isNotAttack) {
                                balkRight = NO_ATTACK_BALK
                            } else if (isItemBalk && balkRight != NO_ATTACK_BALK) {
                                balkRight = 1
                            }
                        }

                        else -> {
                            if (balkDown != NO_ATTACK_BALK && isNotAttack) {
                                balkDown = NO_ATTACK_BALK
                            } else if (isItemBalk && balkDown != NO_ATTACK_BALK) {
                                balkDown = 1
                            }
                        }
                    }
                }
            }
            index++
        }
        if(player.id == "player2-xxx")log.info("Number of balk= ${listOf(balkLeft, balkUp, balkRight, balkDown).filter { it != NO_ATTACK_BALK }.sum()}")
        return listOf(balkLeft, balkUp, balkRight, balkDown).filter { it != NO_ATTACK_BALK }.sum()
//        for (i in 0 until 4) {
//            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
//
//            if (checkPositionInbound(nextPosition) && checkPositionIsItem(nextPosition, item = ItemType.BALK)) {
//                return true
//            }
//        }
//        return false
    }

    private fun checkPositionInbound(position: Position): Boolean {
        return position.row >= 0 && position.row < mapInfo.size.rows && position.col >= 0 && position.col < mapInfo.size.cols
    }

    private fun checkPositionIsItem(position: Position, item: ItemType): Boolean =
        mapInfo.map.getOrNull(position.row)?.getOrNull(position.col) == item

    val lengthOfBomb: Int get() = (player.power).coerceAtLeast(1)


    fun checkPositionIsSafe(position: Position): Boolean {
        if (!checkPositionIsInbound(position)) return false
        return !checkIsNearBomb(position, noCheckTime = true)
    }

    fun checkForceMoveOverBomb(
        position: Position,
        timeOfCurrentBomb: Long = 0, // Time of bomb, that place at player.
    ): Boolean {
        var index = 0
        var bombExposedEarliest = Long.MIN_VALUE
        val otherBombs = mutableSetOf<Long>()
        while (index <= lengthOfBomb) {
            for (i in dx.indices) {
                val x = position.row + dx[i] * index
                val y = position.col + dy[i] * index
                // 1: timestamp = 102000, bomb dropped 102000, endTime = 104000
                if (x >= 0 && x < mapInfo.size.rows && y >= 0 && y < mapInfo.size.cols) {
                    if (bombs[x][y] + BUFFER_TIME_END_OF_BOMB > timestamp) {
                        bombExposedEarliest = maxOf(bombExposedEarliest, bombs[x][y])
                        otherBombs.add(bombs[x][y])
                    }
                }
            }
            index++
        }
        if (bombExposedEarliest == Long.MIN_VALUE) return false
        if (timeOfCurrentBomb > 0) {
            log.warning("Position=$position")
            log.warning("Earliest = $bombExposedEarliest | current = $timeOfCurrentBomb, distance = ${timeOfCurrentBomb - bombExposedEarliest}")
            log.warning("other bomb = ${otherBombs}")
        }
        otherBombs.toList().fold(0L) { s, n -> s - n }
        return timeOfCurrentBomb == bombExposedEarliest && otherBombs.size == 1 ||
                (otherBombs.size >1 && abs((otherBombs.firstOrNull()?: 0L)  - (otherBombs.lastOrNull()?:0L)) in 0..300)
    }

    fun getTimeOfBomb(position: Position): Long {
        var index = 0
        var bombExposedEarliest = Long.MAX_VALUE
        while (index <= lengthOfBomb) {
            for (i in dx.indices) {
                val x = position.row + dx[i] * index
                val y = position.col + dy[i] * index
                // 1: timestamp = 102000, bomb dropped 102000, endTime = 104000
                if (x >= 0 && x < mapInfo.size.rows && y >= 0 && y < mapInfo.size.cols) {
                    if (bombs[x][y] + BUFFER_TIME_END_OF_BOMB > timestamp) {
                        bombExposedEarliest = minOf(bombExposedEarliest, bombs[x][y])
                        if (index == 0) return bombExposedEarliest
                    }
                }
            }
            index++
        }
        return if (bombExposedEarliest == Long.MAX_VALUE) {
            0
        } else {
            bombExposedEarliest
        }
    }

    /**
     * Check position is near bomb.
     */
    fun checkIsNearBomb(
        position: Position,
        noCheckTime: Boolean = false,
        timeOfCurrentBomb: Long = 0, // If this value > 0 => check force move over bomb
        avoidBomb: Boolean = false,
    ): Boolean {
        var index = 0
        var bombExposedEarliest = Long.MAX_VALUE
        while (index <= lengthOfBomb) {
            for (i in dx.indices) {
                val x = position.row + dx[i] * index
                val y = position.col + dy[i] * index
                // 1: timestamp = 102000, bomb dropped 102000, endTime = 104000
                if (x >= 0 && x < mapInfo.size.rows && y >= 0 && y < mapInfo.size.cols) {
                    if (bombs[x][y] + BUFFER_TIME_END_OF_BOMB > timestamp) {
                        if (noCheckTime) return true
                        bombExposedEarliest = minOf(bombExposedEarliest, bombs[x][y])
                    }
                }
            }
            index++
        }
        // time = 12000, bomb = 14000
        // time = 13000, bomb = 14000
        if (bombExposedEarliest == Long.MAX_VALUE) return false
        if (timeOfCurrentBomb <= bombExposedEarliest) return true
//        //ln("Remain time nocheck")
//        //ln("Avoid && bomb")
//        if (avoidBomb && bombExposedEarliest - timestamp > 1000) return false
//        //ln("bombExposedEarliest + BUFFER_TIME_END_OF_BOMB")
        return bombExposedEarliest + BUFFER_TIME_END_OF_BOMB > timestamp
    }

    fun checkCanMove(position: Position): Boolean {
        val item = mapInfo.map[position.row][position.col]
        val spoilItem = mapInfo.spoils.firstOrNull { it.row == position.row && it.col == position.col }
        return !listOf(
            ItemType.BALK,
            ItemType.WALL,
            ItemType.QUARANTINE_PLACE,
            ItemType.TELEPORT_GATE,
            ItemType.DRAGON_EGG_GST
        ).contains(item) && spoilItem?.spoilType != SpoilType.MYSTIC_DRAGON_EGG
    }

    fun checkCanMoveSafe(position: Position): Boolean {
        for (i in dx.indices) {
            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])
            if (checkPositionIsSafe(nextPosition) && checkCanMove(nextPosition)) return true
        }
        return false
    }

    /**
     * Check player is near bomb.
     */
    fun checkIsNearBomb(noCheckTime: Boolean = false): Boolean {
        return checkIsNearBomb(player.currentPosition, noCheckTime = noCheckTime)
    }

    fun checkPositionIsInbound(position: Position): Boolean {
        return position.row >= 0 && position.row < mapInfo.size.rows && position.col >= 0 && position.col < mapInfo.size.cols
    }

    fun checkSpoilNeedGet(position: Position, spoilsNeedGet: List<SpoilType>): Boolean {
        return mapInfo.spoils.any { spoil ->
            spoil.spoilType in spoilsNeedGet && spoil.row == position.row && spoil.col == position.col
        }
    }

    fun getItem(position: Position): ItemType {
        return mapInfo.map[position.row][position.col]
    }

    override fun toString(): String {
        return copy(mapInfo = mapInfo.copy(map = emptyList())).toJson().toString()
    }

    companion object {
        private const val MAX_LENGTH_ATTACK = 5
        private const val MIN_LENGTH_ATTACK = 2
        private const val BUFFER_TIME_END_OF_BOMB = 800L
        private const val BOMB_EXPOSED_TIME = 2000L
        private const val NO_ATTACK_BALK = -1
        private val log = Logger.getLogger(BotExecutor::class.java.name)
    }
}

enum class GameTag(val value: String) {
    PLAYER_BANNED("player:moving-banned"), // moving is blocking, two player moves to same cell
    PLAYER_START_MOVING("player:start-moving"), PLAYER_STOP_MOVING("player:stop-moving"), PLAYER_ISOLATED("player:be-isolated"), // player blocked in quarantine area
    PLAYER_BACK_TO_PLAY("player:back-to-playground"), // Player was moved out quarantine area, and stand on floor
    PLAYER_PICK_SPOIL("player:pick-spoil"), BOMB_EXPLOSED("bomb:explosed"), BOMB_SETUP("bomb:setup"), START_GAME("start-game"), UPDATE_DATA(
        "update-data"
    ),
    PLAYER_JOINED("player-joined"),
}

class GameTagAdapter {
    @FromJson
    fun fromJson(tag: String): GameTag? {
        return GameTag.values().firstOrNull { it.value == tag }
    }
}