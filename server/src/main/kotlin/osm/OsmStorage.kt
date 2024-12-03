package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.*
import java.io.File


class OsmStorage(
    val nodes: MutableMap<Long, OsmNode>,
    val ways: MutableMap<Long, OsmWay>,
    val relations: MutableMap<Long, OsmRelation>
) {
    lateinit var buildings: HashSet<Building>
    lateinit var roads: HashSet<Road>


    constructor(source: File) : this(mutableMapOf(), mutableMapOf(), mutableMapOf()) {
        val iter: Iterator<EntityContainer> = PbfIterator(source.inputStream(), true).iterator()
        processOsmEntities(iter)
        buildings = createBuildingSet()
        roads = createRoadSet()
        calculateCapacities()
    }

    private fun createBuildingSet(): HashSet<Building> {
        val default = nodes.filter { it.value.isBuilding() }
        val wayRelated = ways.filter { it.value.isBuilding() }
        val relationRelated = relations.filter { it.value.isBuilding() }
        val output = mutableSetOf<Building>()
        default.forEach { node ->
            output.add(Building(node.value.id, node.value.tags.map { SerializableTag(it) }, EntityType.Node, Vec3(node.value.latitude, 0.0, node.value.longitude), emptyList()))
        }
        wayRelated.forEach { way ->
            output.add(Building(way.value.id, way.value.tags.map { SerializableTag(it) }, EntityType.Way, way.value.nodeIds.map { nodes[it]!! }
                .nodeAverage(), listOf(SerializableWay(way.value, way.value.nodeIds.map { nodes[it]!! }))))
        }
        relationRelated.forEach { relation ->
            output.add(
                Building(
                    relation.value.id, relation.value.tags.map { SerializableTag(it) }, EntityType.Relation,
                    relation.value.members.filter { it.type == EntityType.Way }.flatMap { w -> ways[w.id]!!.nodeIds.mapNotNull { nodes[it] } }.nodeAverage(),
                    relation.value.members.filter { it.type == EntityType.Way }.firstOrNull { it.role == "outer"}
                        ?.let { w -> listOf(SerializableWay(ways[w.id]!!, ways[w.id]!!.nodeIds.mapNotNull { nodes[it] }))} ?: listOf()
                )
            )
        }
        return output.toHashSet()
    }

    private fun createRoadSet(): HashSet<Road> {
        val elements = ways.filter { it.value.isRoad() }
        val output = mutableSetOf<Road>()

        elements.forEach { way ->
            output.add(Road(
                way.key, way.value.tags.map { SerializableTag(it) }, listOf(SerializableWay(way.value, way.value.nodeIds.map { nodes[it]!! })))
            )
        }
        return output.toHashSet()
    }

    private fun calculateCapacities() {
        buildings.forEach { it.capacity = it.calculateCapacity() }
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