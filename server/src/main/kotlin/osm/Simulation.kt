package dev.voroscsoki.stratopolis.server.osm

import dev.voroscsoki.stratopolis.common.SimulationData
import dev.voroscsoki.stratopolis.common.elements.AgeGroup
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.networking.SimulationResult
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import dev.voroscsoki.stratopolis.server.Main
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Serializable
class Distributions {
    var ageToType: Map<AgeGroup, Map<String, Double>> = mapOf()
    var typeToTime: Map<String, List<ChanceInterval>> = mapOf()

    fun getDistribution(age: AgeGroup, time: LocalDateTime): Map<String, Double> {
        val type = ageToType[age]
        return type?.map {
            it.key to it.value * (typeToTime[it.key]?.getForHour(time.hour) ?: 0.0)
        }?.toMap() ?: mapOf()
    }

    private fun List<ChanceInterval>.getForHour(hour: Int): Double {
        for (interval in this) {
            if (hour in interval.start..interval.end) return interval.chance
            if(interval.start > interval.end && (hour in interval.start..23 || hour in 0..interval.end)) return interval.chance
        }
        return this.first().chance
    }
}

@Serializable
data class ChanceInterval(val start: Int, val end: Int, val chance: Double)

class Simulation {
    var clock = Clock.System.now()
    val agentSpeed = 0.0000001
    val agents = mutableListOf<Agent>()
    val distributions = File("distributions.json").let {
        if (it.exists()) Json {allowStructuredMapKeys = true}.decodeFromString(Distributions.serializer(), it.readText())
        else Distributions()
    }

    private fun setup(count: Int) {
        agents.clear()
        val bldg = DatabaseAccess.getRandomBuildings(count * 2)
        for (i in 0..<count) {
            agents += Agent(
                Random.nextLong().absoluteValue,
                bldg[i],
                bldg[i + 1],
                bldg[i].coords)
            println(i)
        }
    }

    fun tick(callback: (Vec3) -> Unit) {
        clock += 1.minutes
        agents.map { ag ->

            repeat(60) {
                if(ag.id == 1L) println((ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat())))
                ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords).normalize() * (ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat()))
                if (ag.location dist ag.targetBuilding.coords < 0.00000001) {
                    ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
                    ag.location = ag.atBuilding.coords
                }
                callback(ag.location)
            }

        }
    }

    fun startSimulation(startingData: SimulationData) {
        val res = startingData.copy()
        setup(res.agentCount)
        clock = res.startTime
        while (clock < startingData.endTime) {
            tick {
                res.addFrequency(it)
            }
        }
        Main.socketServer.sendSocketMessage(SimulationResult(res))
    }
}