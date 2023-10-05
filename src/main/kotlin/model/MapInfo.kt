package model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MapInfo(
    val bombs: List<Bomb>,
    val gameStatus: Any?,
    val human: List<Any>?,
    val humanSpeed: Int?,
    val map: List<List<ItemType>>?,
    val players: List<Player>?,
    val size: Size?,
    val spoils: List<Spoil>?,
    val virusSpeed: Int?,
    val viruses: List<Any>?
)

enum class ItemType(val value: Int) {
    ROAD(0),
    WALL(1),
    BALK(2),
    TELEPORT_GATE(7),
    QUARANTINE_PLACE(8),
    DRAGON_EGG_GST(9),
    MYSTIC_DRAGON_EGG(6),
    ATTACK_DRAGON_EGG(4),
    DELAY_TIME_DRAGON_EGG(5),
    SPEED_DRAGON_EGG(3)
}

class ItemAdapter {
    @FromJson
    fun toItemType(map: List<List<Int>>) = map.map { it.map(::itemTypeOf) }

    private fun itemTypeOf(mapItem: Int): ItemType =
        ItemType.values().firstOrNull { it.value == mapItem } ?: ItemType.ROAD
}