package dev.voroscsoki.stratopolis.common.elements

import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.iface.OsmWay
import dev.voroscsoki.stratopolis.common.util.nodeIds
import dev.voroscsoki.stratopolis.common.util.tags
import kotlinx.serialization.Serializable

@Serializable
class SerializableWay(val id: Long, val tags: List<SerializableTag>, val nodes: List<SerializableNode>) {
    constructor(wrappedWay: OsmWay, nodes: List<OsmNode>) : this(wrappedWay.id, wrappedWay.tags.map { SerializableTag(it) }, wrappedWay.nodeIds.map { id -> SerializableNode(nodes.first { it.id == id }) })
}