package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.OsmNode
import kotlinx.serialization.Serializable

@Serializable
class SerializableNode(val id: Long, val tags: List<SerializableTag>) {
    constructor(wrappedNode: OsmNode) : this(wrappedNode.id, wrappedNode.tags.map { SerializableTag(it) })
}