package model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Player(
    val attackDragonEgg: Int?,
    val box: Int?,
    val currentPosition: Position?,
    val delay: Int?,
    val dragonEggAttack: Int?,
    val dragonEggDelay: Int?,
    val dragonEggMystic: Int?,
    val dragonEggSpeed: Int?,
    val humanCured: Int?,
    val humanInfected: Int?,
    val humanSaved: Int?,
    val id: String?,
    val lives: Int?,
    val pill: Int?,
    val pillUsed: Int?,
    val power: Int?,
    val quarantine: Int?,
    val score: Int?,
    val spawnBegin: SpawnBegin?,
    val speed: Int?,
    val virus: Int?,
    val virusInfected: Int?
)