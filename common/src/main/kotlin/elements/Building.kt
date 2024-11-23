package dev.voroscsoki.stratopolis.common.elements
import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.serialization.Serializable

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val type: EntityType,
    val coords: Vec3 = Vec3(0f,0f,0f),
    val ways: List<SerializableWay>,
)