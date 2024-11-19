package dev.voroscsoki.stratopolis.common.elements

import de.topobyte.osm4j.core.model.iface.OsmNode
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.tags
import kotlinx.serialization.Serializable

@Serializable
data class SerializableNode(val id: Long, val tags: List<SerializableTag>, val coords: Vec3) {
    constructor(wrappedNode: OsmNode) : this(wrappedNode.id, wrappedNode.tags.map { SerializableTag(it) }, Vec3(wrappedNode.latitude.toFloat(),0f, wrappedNode.longitude.toFloat()))
}