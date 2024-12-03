package dev.voroscsoki.stratopolis.common.elements
import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.pow

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val osmType: EntityType,
    val coords: Vec3 = Vec3(0.0,0.0,0.0),
    val ways: List<SerializableWay>,
    val buildingType: String? = tags.firstOrNull { it.key == "building" }?.value,
    var capacity: UInt = 0u
) {

    fun height(): Float {
        return tags.firstOrNull { it.key == "height" }?.value?.toFloatOrNull()
            ?: tags.firstOrNull { it.key == "building:levels" }?.value?.toFloatOrNull()
            ?: 2f
    }

    //https://rosettacode.org/wiki/Shoelace_formula_for_polygonal_area#Kotlin
    private fun area(): Double {
        val allPoints = ways.flatMap { it.nodes }.map { it.coords }
        if(allPoints.isEmpty()) return 10.0.pow(-8.0)
        var area = 0f
        for (i in 0 until allPoints.size - 1) {
            area += (allPoints[i].x * allPoints[i + 1].z - allPoints[i + 1].x * allPoints[i].z).toFloat()
        }
        return abs(area + allPoints[allPoints.size - 1].x * allPoints[0].z - allPoints[0].x * allPoints[allPoints.size -1].z) / 2.0
    }

    fun calculateCapacity(): UInt {
        return (area() * height() * 1.5 * 10.0.pow(8)).toUInt()
    }
}