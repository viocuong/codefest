package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Bomb(
    val row: Int,
    val col: Int,
    val remainTime: Int
)