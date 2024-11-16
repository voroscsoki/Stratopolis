package dev.voroscsoki.stratopolis.server.osm

import de.topobyte.osm4j.core.model.iface.OsmEntity
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.nodeIds
import dev.voroscsoki.stratopolis.common.util.tags

fun OsmEntity.isBuilding(): Boolean {
    return this.tags.any { it.key == "building" }
}

fun OsmEntity.isRoad(): Boolean {
    return this is OsmWay && this.tags.any { it.key == "highway" }
}

fun List<OsmNode>.nodeAverage() : Vec3 {
    val coords = this.map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average().toFloat()
    val avgLon = coords.map { it.second }.average().toFloat()
    return Vec3(avgLat,0f,avgLon)
}

fun List<OsmWay>.wayAverage(nodes: Map<Long, OsmNode>) : Vec3 {
    val coords = this.map { way -> way.nodeIds.map { nodes[it]!! } }.flatten().map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average().toFloat()
    val avgLon = coords.map { it.second }.average().toFloat()
    return Vec3(avgLat,0f,avgLon)
}