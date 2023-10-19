package bot.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class Spoil(
    val col: Int,
    val row: Int,
    @Json(name = "spoil_type")
    val spoilType: SpoilType?
)

enum class SpoilType(val value: Int) {
    SPEED_DRAGON_EGG(3),
    ATTACK_DRAGON_EGG(4),
    DELAY_TIME_DRAGON_EGG(5),
    MYSTIC_DRAGON_EGG(6)
}

class SpoilTypeAdapter {
    @FromJson
    fun fromJson(spoilType: Int): SpoilType? {
        return SpoilType.values().firstOrNull { it.value == spoilType }
    }
}