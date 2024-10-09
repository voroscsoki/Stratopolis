package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.*


val OsmEntity.tags: List<OsmTag>
    get() = (0..<this.numberOfTags).map { this.getTag(it) }

val OsmRelation.members: List<OsmRelationMember>
    get() = (0..<this.numberOfMembers).map { this.getMember(it) }

val OsmWay.nodeIds: List<Long>
    get() = (0..<this.numberOfNodes).map { this.getNodeId(it) }

fun List<OsmNode>.getAverage() : CoordPair {
    val sum = this.fold(Pair(0.0, 0.0)) { acc, node -> Pair(acc.first + node.latitude, acc.second + node.longitude) }
    return Pair(sum.first / this.size, sum.second / this.size)
}