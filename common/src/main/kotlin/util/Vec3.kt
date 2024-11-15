package dev.voroscsoki.stratopolis.common.util

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

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

    infix fun dist(other: Vec3): Double {
        return sqrt((other.x - this.x).pow(2.0) + (other.y - this.y).pow(2.0) + (other.z - this.z).pow(2.0))
    }
    infix operator fun plus(other: Vec3): Vec3 {
        return Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    infix operator fun minus(other: Vec3): Vec3 {
        return Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    infix operator fun times(amount: Double): Vec3 {
        return Vec3(this.x * amount, this.y * amount, this.z * amount)
    }

    fun normalize(): Vec3 {
        return Vec3(x / length(), y / length(), z / length())
    }

    private fun length(): Double {
        return sqrt(x * x + y * y + z * z)
    }
}