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
    TELEPORT_GATE(3),
    QUARANTINE_PLACE(4),
    DRAGON_EGG_GST(5),
}

class ItemAdapter {
    @FromJson
    fun toItemType(map: List<List<Int>>) = map.map { it.map(::itemTypeOf) }

    private fun itemTypeOf(mapItem: Int): ItemType =
        ItemType.values().firstOrNull { it.value == mapItem } ?: ItemType.ROAD
}