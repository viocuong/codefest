package bot.model

import bot.dx
import bot.dy
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import utils.JsonConverter.toJson
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
    @Transient val playerId: String? = ""
) {
    val player: Player get() = mapInfo.players.first { it.id == playerId }
    val competitor: Player get() = mapInfo.players.first { it.id != playerId }

    val playerBombs: List<Bomb> get() = mapInfo.bombs.filter { it.playerId == playerId }
    val competitorBombs: List<Bomb> get() = mapInfo.bombs.filter { it.playerId != playerId }
    val isActionOfPlayer: Boolean get() = playerIdOfAction == playerId
    fun checkPlayerAtBombPos(): Boolean {
        return checkIsNearBomb()
    }

    fun checkPositionIsNearCompetitorEgg(position: Position): Boolean {
        val competitorEgg = mapInfo.dragonEggGSTArray.firstOrNull { it.id != playerId } ?: return false
        val canVerticalAttack =
            (position.col == competitorEgg.col && abs(position.row - competitorEgg.row) < lengthOfBomb)
        val canHorizontalAttack =
            (position.row == competitorEgg.row && abs(position.col - competitorEgg.col) < lengthOfBomb)
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

    fun checkPositionIsNearBalk(position: Position): Boolean {
//        val length = lengthOfBomb
//        var atLeft: Boolean? = null
//        var atTop: Boolean? = null
//        var atRight: Boolean? = null
//        var atBottom: Boolean? = null
//        for (i in 1 until length) {
//            val left = position.col - i
//            val right = position.col + i
//            val top = position.row - i
//            val bottom = position.row + i
//            atLeft = atLeft != false && checkPositionIsItem(
//                Position(row = position.row, col = left),
//                item = ItemType.BALK
//            )
//            atRight = atRight != false &&
//                    checkPositionIsItem(Position(row = position.row, col = right), item = ItemType.BALK)
//            atTop = atTop != false && checkPositionIsItem(Position(col = position.col, row = top), item = ItemType.BALK)
//            atBottom = atBottom != false && checkPositionIsItem(
//                Position(col = position.col, row = bottom),
//                item = ItemType.BALK
//            )
//        }
//        return atLeft == true || atRight == true || atTop == true || atBottom == true
        for (i in 0 until 4) {
            val nextPosition = Position(row = position.row + dx[i], col = position.col + dy[i])

            if (checkPositionInbound(nextPosition) && checkPositionIsItem(nextPosition, item = ItemType.BALK)) {
                return true
            }
        }
        return false
    }

    /**
     * Return a bomb located on the player position.
     */
    fun getBombAtPlayerPosition(): Bomb? {
        return playerBombs.firstOrNull { player.currentPosition.row == it.row || player.currentPosition.col == it.col }
    }

    private fun checkPositionInbound(position: Position): Boolean {
        return position.row >= 0 && position.row < mapInfo.size.rows && position.col >= 0 && position.col < mapInfo.size.cols
    }

    private fun checkPositionIsItem(position: Position, item: ItemType): Boolean =
        mapInfo.map.getOrNull(position.row)?.getOrNull(position.col) == item

    private val lengthOfBomb: Int get() = player.power + 1

    /**
     * Check position is near bomb.
     */
    fun checkIsNearBomb(position: Position, checkCanMoveBombByTime: Boolean = false): Boolean {
        return mapInfo.bombs.any { bomb ->
            val isNearBom =
                (position.row == bomb.row && abs(position.col - bomb.col) < lengthOfBomb) || (position.col == bomb.col && abs(
                    position.row - bomb.row
                ) < lengthOfBomb)
            val canMoveOverBomb = bomb.remainTime >= 600
            if (checkCanMoveBombByTime) {
                isNearBom && (!canMoveOverBomb)
            } else {
                isNearBom
            }

        }
    }

    fun getBombRemainTimePlayer(): Int {
        return mapInfo.bombs.filter { it.playerId == playerId }.maxOfOrNull { it.remainTime } ?: -1
    }

    /**
     * Check player is near bomb.
     */
    fun checkIsNearBomb(): Boolean {
        return checkIsNearBomb(player.currentPosition)
    }

    fun checkPositionIsInbound(position: Position): Boolean {
        return position.row >= 0 && position.row < mapInfo.size.rows && position.col >= 0 && position.col < mapInfo.size.cols
    }

    fun checkSpoilNeedGet(position: Position, spoilsNeedGet: List<SpoilType>): Boolean {
        return mapInfo.spoils.any { spoil ->
            spoil.spoilType in spoilsNeedGet && spoil.row == position.row && spoil.col == position.col
        }
    }

    override fun toString(): String {
        return copy(mapInfo = mapInfo.copy(map = emptyList())).toJson().toString()
    }

    companion object {
        private const val MAX_LENGTH_ATTACK = 5
        private const val MIN_LENGTH_ATTACK = 2
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