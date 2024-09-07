package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.*


fun OsmEntity.getTags(): List<OsmTag> {
    return (0..<this.numberOfTags).map { this.getTag(it) }
}
fun OsmRelation.getMembers() : List<OsmRelationMember> {
    return (0..<this.numberOfMembers).map { this.getMember(it) }
}
fun OsmWay.getNodeIds() : List<Long> {
    return (0..<this.numberOfNodes).map { this.getNodeId(it) }
}
fun List<OsmNode>.getAverage() : CoordPair {
    val sum = this.fold(Pair(0.0, 0.0)) { acc, node -> Pair(acc.first + node.latitude, acc.second + node.longitude) }
    return Pair(sum.first / this.size, sum.second / this.size)
}