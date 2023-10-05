package model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import utils.JsonConverter.toJson

@JsonClass(generateAdapter = true)
data class GameInfo(
    val gameRemainTime: Int,
    val id: Int,
    @Json(name = "map_info")
    val mapInfo: MapInfo,
    val tag: GameTag,
    val timestamp: Long,
){
    override fun toString(): String {
        return toJson().toString()
    }
}

enum class GameTag(val value: String) {
    PLAYER_BANNED("player:moving-banned"), // moving is blocking, two player moves to same cell
    PLAYER_START_MOVING("player:start-moving"),
    PLAYER_STOP_MOVING("player:stop-moving"),
    PLAYER_ISOLATED("player:be-isolated"), // player blocked in quarantine area
    PLAYER_BACK_TO_PLAY("player:back-to-playground"), // Player was moved out quarantine area, and stand on floor
    PLAYER_PICK_SPOIL("player:pick-spoil"),
    BOMB_EXPLOSED("bomb:explosed"),
    BOMB_SETUP("bomb:setup"),
    START_GAME("start-game"),
    UPDATE_DATA("update-data"),
}

class GameTagAdapter {
    @FromJson
    fun fromJson(tag: String): GameTag? {
        return GameTag.values().firstOrNull { it.value == tag }
    }
}