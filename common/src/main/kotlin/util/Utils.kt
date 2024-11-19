package dev.voroscsoki.stratopolis.common.util

import de.topobyte.osm4j.core.model.iface.*


val OsmEntity.tags: List<OsmTag>
    get() = (0..<this.numberOfTags).map { this.getTag(it) }

val OsmRelation.members: List<OsmRelationMember>
    get() = (0..<this.numberOfMembers).map { this.getMember(it) }

val OsmWay.nodeIds: List<Long>
    get() = (0..<this.numberOfNodes).map { this.getNodeId(it) }

fun List<OsmNode>.getAverage() : Vec3 {
    val sum = this.fold(Pair(0f, 0f)) { acc, node -> Pair(acc.first + node.latitude.toFloat(), acc.second + node.longitude.toFloat()) }
    return Vec3(sum.first / this.size,0f, sum.second / this.size)
}

fun getMemoryUsage(): Long {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
}