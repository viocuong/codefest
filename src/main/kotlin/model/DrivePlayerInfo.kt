package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DrivePlayerInfo(
    val direction: String,
    val playerId: String
)