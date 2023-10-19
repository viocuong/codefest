package bot.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Player(
    val box: Int,
    var currentPosition: Position,
    val delay: Int,
    val dragonEggAttack: Int,
    val dragonEggDelay: Int,
    val dragonEggMystic: Int,
    val dragonEggMysticAddEgg: Int,
    val dragonEggMysticIsolateGate: Int,
    val dragonEggMysticMinusEgg: Int,
    val dragonEggSpeed: Int,
    val gstEggBeingAttacked: Int,
    val id: String,
    val lives: Int,
    val power: Int,
    val quarantine: Int,
    val score: Int,
    val spawnBegin: Position,
    val speed: Int
)