package bot.model

import bot.dx
import bot.dy
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import utils.JsonConverter.toJson
import kotlin.math.abs

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
        return mapInfo.bombs.filter { it.playerId == playerId }
            .any { it.col == player.currentPosition.col && it.row == player.currentPosition.row }
    }

    fun checkPositionIsNearBalk(position: Position): Boolean {
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
        mapInfo.map[position.row][position.col] == item


    /**
     * Check position is near bomb.
     */
    fun checkIsNearBomb(position: Position): Boolean {
        val distancePlayerToBomb =
            player.dragonEggAttack.coerceAtMost(MAX_LENGTH_ATTACK).coerceAtLeast(MIN_LENGTH_ATTACK)
        return mapInfo.bombs.any { bomb ->
            (position.row == bomb.row && abs(position.col - bomb.col + 1) <= distancePlayerToBomb) || (position.col == bomb.col && abs(
                position.row - bomb.row + 1
            ) <= distancePlayerToBomb)
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
        private const val MAX_LENGTH_ATTACK = 4
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