package bot.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MapInfo(
    val bombs: MutableList<Bomb> = mutableListOf(),
    val dragonEggGSTArray: List<DragonEggGSTArray>,
    val gameStatus: Any?,
    val map: List<List<ItemType>> = emptyList(),
    val players: List<Player>,
    val size: Size,
    val spoils: List<Spoil>,
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
    fun toItemType(map: List<List<Int>>): List<List<ItemType>> = map.map { it.map(::itemTypeOf) }

    private fun itemTypeOf(mapItem: Int): ItemType =
        ItemType.values().firstOrNull { it.value == mapItem } ?: ItemType.ROAD
}