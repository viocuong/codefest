package bot.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Bomb(
    val col: Int,
    val playerId: String,
    val remainTime: Int,
    val row: Int,
    @Transient
    val endExposedTime: Long = 0
){
    val identify: String get() = "$col-$row-$playerId"
}