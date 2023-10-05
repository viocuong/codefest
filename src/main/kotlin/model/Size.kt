package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Size(
    val cols: Int,
    val rows: Int
)