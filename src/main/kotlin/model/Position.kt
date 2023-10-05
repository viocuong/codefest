package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Position(
    val col: Int,
    val row: Int
)