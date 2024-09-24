package api

import de.topobyte.osm4j.core.model.iface.OsmWay
import dev.voroscsoki.stratopolis.common.api.SerializableTag
import dev.voroscsoki.stratopolis.common.api.nodeIds
import dev.voroscsoki.stratopolis.common.api.tags
import kotlinx.serialization.Serializable

@Serializable
class SerializableWay(val id: Long, val tags: List<SerializableTag>, val nodes: List<Long>) {
    constructor(wrappedWay: OsmWay) : this(wrappedWay.id, wrappedWay.tags.map { SerializableTag(it) }, wrappedWay.nodeIds)
}