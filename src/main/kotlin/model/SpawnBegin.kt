package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpawnBegin(
    val col: Int,
    val row: Int
)