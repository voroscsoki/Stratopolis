package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.OsmNode
import kotlinx.serialization.Serializable

@Serializable
class SerializableNode(val id: Long, val tags: List<SerializableTag>, val coords: Vec3) {
    constructor(wrappedNode: OsmNode) : this(wrappedNode.id, wrappedNode.tags.map { SerializableTag(it) }, Vec3(wrappedNode.latitude,0.0, wrappedNode.longitude))
}