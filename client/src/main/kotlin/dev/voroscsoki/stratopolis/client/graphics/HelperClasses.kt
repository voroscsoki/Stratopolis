package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.coroutines.sync.Mutex

open class GraphicalObject(val instance: ModelInstance) {
    fun isVisible(cam: PerspectiveCamera): Boolean {
        val position = this.instance.transform.getTranslation(Vector3())
        return cam.frustum.sphereInFrustum(position, 1f)
    }
}

class GraphicalBuilding(val apiData: Building?, instance: ModelInstance) : GraphicalObject(instance)
class GraphicalArrow(var location: Vec3, instance: ModelInstance) : GraphicalObject(instance)
class GraphicalRoad(val apiData: Road?, instance: ModelInstance) : GraphicalObject(instance)

data class CacheObject(val cache: ModelCache, val lock: Mutex, val startingCoords: Vector3, val size: Int, var isVisible: Boolean = false) {
    fun checkVisibility(cam: PerspectiveCamera) {
        val resolution = 32
        for (x in 0..resolution) {
            for (z in 0..resolution) {
                val point = Vector3(
                    startingCoords.x + (x.toFloat()/resolution) * size,
                    startingCoords.y,
                    startingCoords.z + (z.toFloat()/resolution) * size
                )
                if (cam.frustum.pointInFrustum(point)) {
                    isVisible = true
                    return
                }
            }
        }
        isVisible = false
    }
}