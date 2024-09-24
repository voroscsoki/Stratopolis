package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.common.api.tags
import java.io.File


class OsmStorage(val source: File) {
    companion object {
        val nodes = mutableMapOf<Long, SerializableNode>()
        val buildings = mutableListOf<Building>()
    }
    private var iter: Iterator<EntityContainer> = PbfIterator(source.inputStream(), true).iterator()

    init {
        processOsmEntities(iter)
        println("Nodes: ${nodes.size}")
    }

    private fun processOsmEntities(iter: Iterator<EntityContainer>) {
        for (entity in iter) {
            when (val osmEntity = entity.entity) {
                is OsmNode -> handleNode(osmEntity)
                is OsmWay -> handleWay(osmEntity)
                is OsmRelation -> handleRelation(osmEntity)
            }
        }
    }


    private fun handleNode(entity: OsmNode) {
        nodes[entity.id] = SerializableNode(entity)
    }

    private fun handleWay(entity: OsmWay) {
        //skip
    }
    private fun handleRelation(entity: OsmRelation) {
        //skip
    }
}

fun OsmEntity.isBuilding(): Boolean {
    return this.tags.any { it.key == "building" }
}
