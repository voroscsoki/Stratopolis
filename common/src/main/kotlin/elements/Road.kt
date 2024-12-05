package dev.voroscsoki.stratopolis.common.elements

import kotlinx.serialization.Serializable

@Serializable
data class Road(
    val id: Long,
    val tags: List<SerializableTag>,
    val ways: List<SerializableWay>
)
