package dev.voroscsoki.stratopolis.common.util

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sqrt

@Serializable
data class Vec3(val x: Float, val y: Float, val z: Float) {
    companion object {
        fun fromString(str: String): Vec3 {
            val parts = str.split(",")
            return Vec3(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
        }
    }

    override fun toString(): String {
        return "$x,$y,$z"
    }

    infix fun dist(other: Vec3): Float {
        return sqrt((other.x - this.x).pow(2f) + (other.y - this.y).pow(2f) + (other.z - this.z).pow(2f))
    }
    infix operator fun plus(other: Vec3): Vec3 {
        return Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    infix operator fun minus(other: Vec3): Vec3 {
        return Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    infix operator fun times(amount: Float): Vec3 {
        return Vec3(this.x * amount, this.y * amount, this.z * amount)
    }

    fun normalize(): Vec3 {
        return Vec3(x / length(), y / length(), z / length())
    }

    private fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }
}