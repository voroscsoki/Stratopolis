package dev.voroscsoki.stratopolis.common.elements

import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.serialization.Serializable

enum class AgeGroup {
    CHILD, TEEN, ADULT, SENIOR
}

@Serializable
data class Agent(
    val id: Long,
    var atBuilding: Building,
    var targetBuilding: Building,
    var location: Vec3,
    val speed: Float = 0.002f,
    val ageGroup: AgeGroup = AgeGroup.ADULT
) {
}