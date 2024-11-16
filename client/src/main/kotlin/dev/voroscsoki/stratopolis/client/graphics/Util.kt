package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.util.Vec3

fun Vec3.toSceneCoords(baselineCoord: Vec3, scaleOnly: Boolean = false): Vec3 {
        if(scaleOnly) {
            val x = this.x * 100000
            val y = this.y * 100000
            val z = this.z * 100000
            return Vec3(x, y, z)
        }
        val x = (this.x - baselineCoord.x) * 100000
        val y = (this.y - baselineCoord.y) * 100000
        val z = (this.z - baselineCoord.z) * 100000
        return Vec3(x, y, z)
    }

fun Vector3.toWorldCoords(baselineCoord: Vec3): Vec3 {
    val x = this.x / 100000 + baselineCoord.x
    val y = this.y / 100000 + baselineCoord.y
    val z = this.z / 100000 + baselineCoord.z
    return Vec3(x, y, z)
}