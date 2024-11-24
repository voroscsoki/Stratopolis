package dev.voroscsoki.stratopolis.common.util

import de.topobyte.osm4j.core.model.iface.*
import dev.voroscsoki.stratopolis.common.elements.SerializableNode


val OsmEntity.tags: List<OsmTag>
    get() = (0..<this.numberOfTags).map { this.getTag(it) }

val OsmRelation.members: List<OsmRelationMember>
    get() = (0..<this.numberOfMembers).map { this.getMember(it) }

val OsmWay.nodeIds: List<Long>
    get() = (0..<this.numberOfNodes).map { this.getNodeId(it) }

fun List<OsmNode>.avg() : Vec3 {
    val sum = this.fold(Pair(0.0, 0.0)) { acc, node -> Pair(acc.first + node.latitude, acc.second + node.longitude) }
    return Vec3(sum.first / this.size,0.0, sum.second / this.size)
}

fun List<SerializableNode>.getAverage() : Vec3 {
    val sum = this.fold(Pair(0.0, 0.0)) { acc, node -> Pair(acc.first + node.coords.x, acc.second + node.coords.z) }
    return Vec3(sum.first / this.size,0.0, sum.second / this.size)
}

fun OsmEntity.isBuilding(): Boolean {
    return this.tags.any { it.key == "building" }
}

fun OsmEntity.isRoad(): Boolean {
    return this.tags.any { it.key == "highway" }
}

fun List<OsmNode>.nodeAverage() : Vec3 {
    val coords = this.map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average()
    val avgLon = coords.map { it.second }.average()
    return Vec3(avgLat,0.0,avgLon)
}

fun List<OsmWay>.wayAverage(nodes: Map<Long, OsmNode>) : Vec3 {
    val coords = this.map { way -> way.nodeIds.map { nodes[it]!! } }.flatten().map { it.latitude to it.longitude }
    val avgLat = coords.map { it.first }.average()
    val avgLon = coords.map { it.second }.average()
    return Vec3(avgLat,0.0,avgLon)
}

fun getMemoryUsage(): Long {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
}