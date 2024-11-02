package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.*


val OsmEntity.tags: List<OsmTag>
    get() = (0..<this.numberOfTags).map { this.getTag(it) }

val OsmRelation.members: List<OsmRelationMember>
    get() = (0..<this.numberOfMembers).map { this.getMember(it) }

val OsmWay.nodeIds: List<Long>
    get() = (0..<this.numberOfNodes).map { this.getNodeId(it) }

fun List<OsmNode>.getAverage() : Vec3 {
    val sum = this.fold(Pair(0.0, 0.0)) { acc, node -> Pair(acc.first + node.latitude, acc.second + node.longitude) }
    return Vec3(sum.first / this.size,0.0, sum.second / this.size)
}

fun getMemoryUsage(): Long {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
}