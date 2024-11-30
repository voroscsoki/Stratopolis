package dev.voroscsoki.stratopolis.common

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SimulationData(val startTime: Instant,
                          val endTime: Instant,
                          val agentCount: Int,
                          val heatmapSquares: MutableMap<String, HeatmapSquare> = mutableMapOf()
) {

    @Serializable
    data class HeatmapSquare(val id: String, var value: Int)
}
