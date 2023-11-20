package bot.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerInfo(
    @Json(name = "game_id")
    val gameId: String,
    @Json(name = "player_id")
    val playerId: String?,
)