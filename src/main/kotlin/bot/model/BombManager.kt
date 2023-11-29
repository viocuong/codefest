package bot.model

class BombManager {
    private var _bombs: MutableList<MutableList<Pair<Long, String>>> =
        mutableListOf() // First is time of bomb, second is

    fun getBombs(currentTimestamp: Long): List<Bomb> {
        val bombsResult = mutableListOf<Bomb>()
        for (i in 0.._bombs.lastIndex) {
            for (j in 0.._bombs[0].lastIndex) {
                val (timeOfBomb, playerId) = _bombs[i][j]
                if (timeOfBomb + TIME_AFTER_DROP >= currentTimestamp) {
                    bombsResult.add(
                        Bomb(
                            row = i,
                            col = j,
                            playerId = playerId,
                            remainTime = (timeOfBomb + TIME_AFTER_DROP - currentTimestamp).toInt()
                        )
                    )
                }
            }
        }
        return bombsResult
    }

    fun init(size: Size) {
        if (_bombs.isEmpty()) {
            _bombs.addAll(List(size.rows) { MutableList(size.cols) { 0L to "" } })
        }
    }

    fun updateBombs(gameInfo: GameInfo) {
        gameInfo.mapInfo.bombs.forEach { bomb ->
            val exposedEndTime = bomb.remainTime + gameInfo.timestamp
            if (_bombs[bomb.row][bomb.col].first < exposedEndTime) {
                _bombs[bomb.row][bomb.col] = exposedEndTime to bomb.playerId
            }
        }
    }

    companion object {
        private const val TIME_AFTER_DROP = 700L
    }
}