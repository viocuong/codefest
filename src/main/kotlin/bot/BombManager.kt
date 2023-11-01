package bot

import bot.model.Bomb
import kotlinx.coroutines.CoroutineScope
import utils.currentTime

/**
 * Manage remain time of bomb, include delay time after bom exposed.
 */
class BombManager {
    private val bombsTime = mutableListOf<Bomb>()
    val bombsNotExposed: List<Bomb> get() = bombsTime.filter { currentTime <= it.endExposedTime }
    fun updateBombs(bombs: List<Bomb>) {
        bombs.filter { it.remainTime == 0 }
        bombsTime.removeIf { it.endExposedTime < currentTime }
        // Remove bomb completely exposed.
        println("BombManager#updateBombs $bombsTime")
    }
    companion object {
        private const val BUFFER_TIME_END_OF_BOMB = 600L
    }
}