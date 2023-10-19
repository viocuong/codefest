package bot.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DragonEggGSTArray(
    val col: Int,
    val id: String,
    val row: Int
)