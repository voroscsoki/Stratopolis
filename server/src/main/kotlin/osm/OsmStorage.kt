package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator

typealias CoordPair = Pair<Double, Double>

class OsmStorage(source: PbfIterator) {
    val buildings = mutableListOf<Building>()
    val seq = source.iterator().asSequence()

    init {
        seq.map { it.entity }.forEach { ent ->
            when (ent) {
                is OsmNode -> {
                    if(ent.isBuilding()) {
                        buildings.add(Building(ent.id, ent.getTags(), ent.metadata, EntityType.Node, Pair(ent.latitude, ent.longitude)))
                    }
                }
            }
        }
        println("Building count: ${buildings.size}")
    }
}

fun OsmEntity.isBuilding(): Boolean {
    return this.getTags().any { it.key == "building" }
}


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
