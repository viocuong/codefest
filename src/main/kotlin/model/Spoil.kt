package model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Spoil(
    val row: Int,
    val col: Int,
    val spoilType: ItemType
)