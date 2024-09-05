package dev.voroscsoki.stratopolis.server

import de.topobyte.osm4j.core.model.iface.OsmEntity
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmRelation
import de.topobyte.osm4j.core.model.iface.OsmRelationMember
import de.topobyte.osm4j.core.model.iface.OsmTag
import de.topobyte.osm4j.core.model.iface.OsmWay
import de.topobyte.osm4j.pbf.seq.PbfIterator
import java.io.File


class OsmReader {
    companion object {
        fun read(filename: String) {
            val fileStream = File(filename).inputStream()

            val osmIterator = PbfIterator(fileStream, true)

            println("Total memory usage: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
            val unit = OsmStorage(osmIterator)
            //print total memory usage
            println("Total memory usage: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
        }
    }
}

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