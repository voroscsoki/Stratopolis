package dev.voroscsoki.stratopolis.server.osm

import api.SerializableWay
import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableTag
import dev.voroscsoki.stratopolis.common.api.members
import dev.voroscsoki.stratopolis.common.api.tags


class OsmStorage(source: PbfIterator) {
    companion object {
        val buildings = mutableListOf<Building>()
        val ways = mutableMapOf<Long, OsmWay>()
    }
    private val seq = source.iterator().asSequence()

    init {
        seq.map { it.entity }.forEach { ent ->
            when (ent) {
                is OsmNode -> {
                    if(ent.isBuilding()) {
                        buildings.add(Building(ent.id, ent.tags.map { SerializableTag(it) }, EntityType.Node, Pair(ent.latitude, ent.longitude)))
                    }
                }
                is OsmWay -> {
                    ways[ent.id] = ent
                    if(ent.isBuilding()) {
                        buildings.add(Building(ent.id, ent.tags.map { SerializableTag(it) }, EntityType.Way, Pair(0.0,0.0), listOf(SerializableWay(ent))))
                    }
                }
                is OsmRelation -> {
                    if(!ent.isBuilding()) return@forEach
                    val members = ent.members
                    val borders = members.filter { it.type == EntityType.Way }
                    buildings.add(Building(ent.id, ent.tags.map { SerializableTag(it) }, ent.type, Pair(0.0,0.0), borders.mapNotNull { ways[it.id]?.let { it1 -> SerializableWay(it1) } }))
                }
            }
        }
        println("Building count: ${buildings.size}")
    }
}

fun OsmEntity.isBuilding(): Boolean {
    return this.tags.any { it.key == "building" }
}
