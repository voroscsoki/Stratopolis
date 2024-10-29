package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.api.*
import java.io.File


class OsmStorage(
    val nodes: MutableMap<Long, OsmNode>,
    val ways: MutableMap<Long, OsmWay>,
    val relations: MutableMap<Long, OsmRelation>
) {
    lateinit var buildings: HashSet<Building>


    constructor(source: File) : this(mutableMapOf(), mutableMapOf(), mutableMapOf()) {
        val iter: Iterator<EntityContainer> = PbfIterator(source.inputStream(), true).iterator()
        processOsmEntities(iter)
        buildings = createBuildingSet()
    }

    private fun createBuildingSet(): HashSet<Building> {
        val default = nodes.filter { it.value.isBuilding() }
        val wayRelated = ways.filter { it.value.isBuilding() }
        val relationRelated = relations.filter { it.value.isBuilding() }
        val output = mutableSetOf<Building>()
        default.forEach { node ->
            output.add(Building(node.value.id, node.value.tags.map { SerializableTag(it) }, EntityType.Node, Vec3(node.value.latitude, 0.0, node.value.longitude)))
        }
        wayRelated.forEach { way ->
            output.add(Building(way.value.id, way.value.tags.map { SerializableTag(it) }, EntityType.Way, way.value.nodeIds.map { nodes[it]!! }
                .nodeAverage(), way.value.nodeIds.map { nodes[it]!! }.map { SerializableNode(it) }))
        }
        relationRelated.forEach { relation ->
            output.add(
                Building(
                    relation.value.id, relation.value.tags.map { SerializableTag(it) }, EntityType.Relation,
                    relation.value.members.filter { it.type == EntityType.Way }.flatMap { w -> ways[w.id]!!.nodeIds.mapNotNull { nodes[it] } }.nodeAverage(),
                    relation.value.members.filter { it is OsmWay }
                        .map { way -> (way as OsmWay).nodeIds.map { nodes[it]!! }.map { SerializableNode(it) } }
                        .flatten()
                )
            )
        }
        return output.toHashSet()
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