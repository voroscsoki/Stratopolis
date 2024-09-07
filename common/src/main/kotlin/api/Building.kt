package dev.voroscsoki.stratopolis.common.api
import api.SerializableWay
import de.topobyte.osm4j.core.model.iface.*
import kotlinx.serialization.*
typealias CoordPair = Pair<Double, Double>

@Serializable
data class Building(
    val id: Long,
    val tags: List<SerializableTag>,
    val type: EntityType,
    val coords: CoordPair = Pair(0.0, 0.0),
    val lines : List<SerializableWay> = emptyList()
)