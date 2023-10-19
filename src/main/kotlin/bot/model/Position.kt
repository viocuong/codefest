package bot.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Position(
    val col: Int,
    val row: Int,
    val command: Command? = null
)