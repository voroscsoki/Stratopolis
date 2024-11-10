package dev.voroscsoki.stratopolis.common.api
import api.SerializableWay
import de.topobyte.osm4j.core.model.iface.EntityType
import kotlinx.serialization.Serializable

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val type: EntityType,
    val coords: Vec3 = Vec3(0.0,0.0,0.0),
    val ways: List<SerializableWay>,
    var occupancy: Int = 0,
)