package dev.voroscsoki.stratopolis.common.elements
import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.serialization.Serializable

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val osmType: EntityType,
    val coords: Vec3 = Vec3(0.0,0.0,0.0),
    val ways: List<SerializableWay>,
    val buildingType: String? = tags.firstOrNull { it.key == "building" }?.value
)