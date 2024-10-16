package dev.voroscsoki.stratopolis.common.api
import api.SerializableWay
import de.topobyte.osm4j.core.model.iface.EntityType
import kotlinx.serialization.Serializable

typealias CoordPair = Pair<Double, Double>

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val type: EntityType,
    val coords: CoordPair = Pair(0.0, 0.0),
    val points : List<SerializableNode> = emptyList()
)