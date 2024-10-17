package dev.voroscsoki.stratopolis.common.api

import kotlinx.serialization.Serializable

@Serializable
data class Vec3(val x: Double, val y: Double, val z: Double) {
    companion object {
        fun fromString(str: String): Vec3 {
            val parts = str.split(",")
            return Vec3(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
        }
    }

    override fun toString(): String {
        return "$x,$y,$z"
    }
}