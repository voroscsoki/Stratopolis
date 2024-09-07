package dev.voroscsoki.stratopolis.common.api
import de.topobyte.osm4j.core.model.iface.*
typealias CoordPair = Pair<Double, Double>

class Building(
    private val id: Long,
    val tags: List<OsmTag>,
    val metadata: OsmMetadata,
    val type: EntityType,
    val coords: CoordPair = Pair(0.0, 0.0),
    val lines : List<OsmWay> = emptyList()
){
    override fun toString(): String {
        return "Building(id=$id, tags=$tags, metadata=$metadata, type=$type, coords=$coords, lines=$lines)"
    }
}