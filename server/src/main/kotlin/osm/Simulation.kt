package dev.voroscsoki.stratopolis.server.osm

import dev.voroscsoki.stratopolis.common.SimulationData
import dev.voroscsoki.stratopolis.common.elements.AgeGroup
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.networking.SimulationResult
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import dev.voroscsoki.stratopolis.server.Main
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
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
    val agents = mutableListOf<Agent>()
    val logger = LoggerFactory.getLogger(this::class.java)
    lateinit var buildingCache: List<Building>
    val distributions = File("distributions.json").let {
        if (it.exists())
            try {
                Json {allowStructuredMapKeys = true}.decodeFromString(Distributions.serializer(), it.readText())
            } catch (e: Exception) {
                logger.warn("Distributions file could not be loaded, starting empty")
                Distributions()
            }
        else Distributions()
    }

    private fun setup(count: Int) {
        logger.info("Setting up simulation with $count agents")
        agents.clear()
        val bldg = DatabaseAccess.getRandomBuildings(count * 3)
        for (i in 0..<count) {
            agents += Agent(
                Random.nextLong().absoluteValue,
                bldg[i],
                bldg[i + 1],
                bldg[i].coords)
        }
        buildingCache = bldg.subList(count*2, count*3)
        logger.info("Setup complete")
    }

    private fun pickNextBuilding(agents: List<Agent>) {
        logger.info("${agents.count()} agents are picking next building")
        //wants to leave at all?
        if(Random.nextBoolean()) return
        //val buildings = DatabaseAccess.getRandomBuildings(agents.count())
        agents.forEach { ag -> ag.targetBuilding = buildingCache.random() }
    }

    fun tick(callback: (List<Vec3>) -> Unit) {
        logger.info("Simulation tick at $clock")
        clock += 1.minutes
        val movesPerMinute = 15
        val needNewBuilding = mutableListOf<Agent>()
        agents.map { ag ->
            val locations = mutableListOf<Vec3>()
            repeat(movesPerMinute) {
                ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords).normalize() * ((ag.speed/movesPerMinute).coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat()))
                if (ag.location dist ag.targetBuilding.coords < 0.00000001) {
                    ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
                    ag.location = ag.atBuilding.coords
                    needNewBuilding += ag
                }
                locations += ag.location
            }
            callback(locations)
        }
        pickNextBuilding(needNewBuilding)
    }

    fun startSimulation(startingData: SimulationData) {
        logger.info("Starting simulation")
        val res = startingData.copy()
        setup(res.agentCount)
        clock = res.startTime
        while (clock < startingData.endTime) {
            tick {
                res.addFrequency(it)
            }
        }
        logger.info("Sending simulation result")
        Main.socketServer.sendSocketMessage(SimulationResult(res))
    }
}