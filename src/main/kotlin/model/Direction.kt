package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Direction(
    val direction: String
)