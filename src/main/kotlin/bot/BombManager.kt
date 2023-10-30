package bot

import bot.model.Bomb
import java.sql.Timestamp

/**
 * Manage remain time of bomb, include delay time after bom exposed.
 */
class BombManager {
    private val bombs = mutableListOf<Bomb>()
    fun updateBombs(bombs: List<Bomb>, timestamp: Long) {
        bombs.filter { it.remainTime == 0 }
    }
}