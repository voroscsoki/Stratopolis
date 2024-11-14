package dev.voroscsoki.stratopolis.common.api

import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: Long,
    var atBuilding: Building,
    var targetBuilding: Building,
    var location: Vec3
)