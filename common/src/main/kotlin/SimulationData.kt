package dev.voroscsoki.stratopolis.common

import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SimulationData(val startTime: Instant,
                          val endTime: Instant,
                          val agentCount: Int, val osmBaseline: Vec3, val osmChunkSize: Double,
) {
    val frequencies: MutableMap<String, Float> = mutableMapOf()

    fun addFrequency(coord: Vec3) {
        //floor the coordinates to the nearest chunk
        //the baseline is a Vec3, so we can use the x and z coordinates to floor the chunk
        val reduced = Vec3(coord.x - osmBaseline.x, 0.0, coord.z - osmBaseline.z)
        val floored = Vec3(reduced.x - reduced.x % osmChunkSize, 0.0, reduced.z - reduced.z % osmChunkSize) + osmBaseline
        frequencies.putIfAbsent(floored.let { "${it.x},${it.z}" }, 0.0f)
        frequencies[floored.let { "${it.x},${it.z}" }] = frequencies[floored.let { "${it.x},${it.z}" }]!! + 1
    }
}
