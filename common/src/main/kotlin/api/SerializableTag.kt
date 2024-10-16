package dev.voroscsoki.stratopolis.common.api

import de.topobyte.osm4j.core.model.iface.*
import kotlinx.serialization.Serializable

@Serializable
class SerializableTag(val key: String, val value: String) {
    constructor(wrappedTag: OsmTag) : this(wrappedTag.key, wrappedTag.value)
}