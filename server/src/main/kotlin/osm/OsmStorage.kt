package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.api.*
import java.io.File
import java.util.Random


class OsmStorage(source: File) {
    companion object {
        val nodes = mutableMapOf<Long, OsmNode>()
        val ways = mutableMapOf<Long, OsmWay>()
        val relations = mutableMapOf<Long, OsmRelation>()
        val buildings: HashSet<Building> by lazy {
            val default = nodes.filter { it.value.isBuilding() }
            val wayRelated = ways.filter { it.value.isBuilding() }
            val relationRelated = relations.filter { it.value.isBuilding() }
            val output = mutableSetOf<Building>()
            val rand = Random()
            default.forEach { node ->
                output.add(Building(node.value.id, node.value.tags.map { SerializableTag(it) }, EntityType.Node, Pair(node.value.latitude, node.value.longitude) ))
            }
            wayRelated.forEach { way ->
                output.add(Building(way.value.id, way.value.tags.map { SerializableTag(it) }, EntityType.Way, way.value.nodeIds.map { nodes[it]!! }.nodeAverage(), way.value.nodeIds.map { nodes[it]!! }.map { SerializableNode(it) }))
            }
            relationRelated.forEach { relation ->
                output.add(Building(relation.value.id, relation.value.tags.map { SerializableTag(it) }, EntityType.Relation,
                    Pair(0.0,0.0),
                    relation.value.members.filter {it is OsmWay }.map { way -> (way as OsmWay).nodeIds.map { nodes[it]!! }.map { SerializableNode(it)} }.flatten()))
            }

            output.toHashSet()
        }
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
        nodes[entity.id] = entity
    }

    private fun handleWay(entity: OsmWay) {
        ways[entity.id] = entity
    }
    private fun handleRelation(entity: OsmRelation) {
        relations[entity.id] = entity
    }
}

fun OsmEntity.isBuilding(): Boolean {
    return this.tags.any { it.key == "building" }
}

fun List<OsmNode>.nodeAverage() : CoordPair {
    val coords = this.map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average()
    val avgLon = coords.map { it.second }.average()
    return avgLat to avgLon
}

fun List<OsmWay>.wayAverage() : CoordPair {
    val coords = this.map { way -> way.nodeIds.map { OsmStorage.nodes[it]!! } }.flatten().map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average()
    val avgLon = coords.map { it.second }.average()
    return avgLat to avgLon
}