package dev.voroscsoki.stratopolis.common.elements

import de.topobyte.osm4j.core.model.iface.OsmTag
import kotlinx.serialization.Serializable

@Serializable
class SerializableTag(val key: String, val value: String) {
    constructor(wrappedTag: OsmTag) : this(wrappedTag.key, wrappedTag.value)

    override fun toString(): String {
        return "$key=$value"
    }
}