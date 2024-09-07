package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableTag
import dev.voroscsoki.stratopolis.common.api.getTags

class OsmStorage(source: PbfIterator) {
    companion object {
        val buildings = mutableListOf<Building>()
    }
    val seq = source.iterator().asSequence()

    init {
        seq.map { it.entity }.forEach { ent ->
            when (ent) {
                is OsmNode -> {
                    if(ent.isBuilding()) {
                        buildings.add(Building(ent.id, ent.getTags().map { SerializableTag(it) }, EntityType.Node, Pair(ent.latitude, ent.longitude)))
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
