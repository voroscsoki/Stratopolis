package dev.voroscsoki.stratopolis.server

import de.topobyte.osm4j.core.model.iface.*
import de.topobyte.osm4j.pbf.seq.PbfIterator

typealias CoordPair = Pair<Double, Double>

class OsmStorage(source: PbfIterator) {
    val buildings = mutableListOf<Building>()
    val ways = mutableMapOf<Long, OsmWay>()
    val relations = mutableSetOf<OsmRelation>()

    init {
        for (item in source) {
            val entity = item.entity
            val tags = entity.getTags()

            if (tags.any { tag -> tag.key == "building" }) {
                when (entity) {
                    is OsmNode -> {
                        buildings.add(Building(entity.id, tags, entity.getMetadata(), entity.getType(), entity.latitude, entity.longitude))
                    }
                    is OsmWay -> {
                        ways[entity.id] = entity
                        buildings.add(Building(entity.id, tags, entity.getMetadata(), entity.getType(), 0.0, 0.0, listOf(entity)))
                    }
                    is OsmRelation -> {
                        relations.add(entity)
                    }
                }
            }
        }

        for (rel in relations) {
            val buildingWays = rel.getMembers()
                .filter { member -> member.type == EntityType.Way }
                .mapNotNull { member -> ways[member.id] }

            if (buildingWays.isNotEmpty()) {
                buildings.add(Building(rel.id, rel.getTags(), rel.metadata, rel.type, lines = buildingWays))
            }
        }

        println("Number of buildings: ${buildings.size}")
    }
}


class Building(
    private val id: Long,
    val tags: List<OsmTag>,
    val metadata: OsmMetadata,
    val type: EntityType,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lines : List<OsmWay> = emptyList()
){
    override fun toString(): String {
        return "Building(id=$id, tags=$tags, metadata=$metadata, type=$type, longitude=$longitude, latitude=$latitude, lines=$lines)"
    }
}

class BuildingBoundary(val points: List<CoordPair>)
