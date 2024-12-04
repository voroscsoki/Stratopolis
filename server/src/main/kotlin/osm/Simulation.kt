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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.time.Duration.Companion.minutes

@Serializable
class Distributions {
    var ageToType: MutableMap<AgeGroup, MutableMap<String, Double>> = mutableMapOf()
    var typeToTime: MutableMap<String, MutableList<ChanceInterval>> = mutableMapOf()

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
    lateinit var buildingCache: MutableMap<String, MutableList<Building>>
    val distributions = File("distributions.json").let {
        if (it.exists())
            try {
                return@let Json {allowStructuredMapKeys = true}.decodeFromString<Distributions>(it.readText())
            } catch (e: Exception) {
                logger.warn("Distributions file could not be loaded, starting empty")
                Distributions()
            }
        else Distributions()
    }

    private fun setup(count: Int) {
        logger.info("Setting up simulation with $count agents")
        agents.clear()
        val starters = DatabaseAccess.getBuildingsByType("apartments", count*4)
        buildingCache = pickBuildingsWeighted(DatabaseAccess.getRandomBuildings(count * 2), count*2).groupBy { it.buildingType }.mapValues { entry -> entry.value.toMutableList() }.toMutableMap()
        for (i in 0..<count) {
            val from = starters[i % starters.size]
            agents += Agent(
                Random.nextLong().absoluteValue,
                from,
                from,
                from.coords)
        }
        pickNextBuilding(agents)
        logger.info("Setup complete")
    }


    private fun pickBuildingsWeighted(buildings: List<Building>, count: Int): List<Building> {
        if (count <= 0 || buildings.isEmpty()) return emptyList()

        // Precompute cumulative weights
        val cumulativeWeights = buildings.map { it.capacity }.runningReduce { acc, capacity -> acc + capacity }
        val totalWeight = cumulativeWeights.last()

        // Generate random numbers for selection
        val randomValues = List(count) { Random.nextUInt(totalWeight) }.sorted()

        val selectedBuildings = mutableListOf<Building>()
        var buildingIndex = 0
        for (random in randomValues) {
            // Find the corresponding building for each random value
            while (random >= cumulativeWeights[buildingIndex]) {
                buildingIndex++
            }
            selectedBuildings.add(buildings[buildingIndex])
        }

        return selectedBuildings
    }

    private fun pickNextBuilding(agents: List<Agent>) {
        logger.info("${agents.count()} agents are picking next building")
        var moreBuildingsNeeded = false
        //val buildings = DatabaseAccess.getRandomBuildings(agents.count())
        agents.forEach { ag ->
            //wants to leave at all?
            if(Random.nextBoolean()) return@forEach
            val distribution = distributions.getDistribution(ag.ageGroup, clock.toLocalDateTime(TimeZone.currentSystemDefault()))
            val type = distribution.toList().shuffled().firstOrNull { Random.nextDouble() < it.second }?.first
            if(type == ag.atBuilding.buildingType) return@forEach
            if(Random.nextDouble() > 0.5) {
                buildingCache[type]?.randomOrNull()?.let { ag.targetBuilding = it; buildingCache[type]?.remove(it) }
                    ?: run { moreBuildingsNeeded = true }
            }
        }
        if(moreBuildingsNeeded) {
            //buildingCache.addAll(DatabaseAccess.getRandomBuildings(agents.count()))
        }
    }

    private fun tick(callback: (List<Vec3>) -> Unit) {
        logger.info("Simulation tick at $clock")
        clock += 1.minutes
        val movesPerMinute = 15
        val needNewBuilding = mutableListOf<Agent>()
        agents.map { ag ->
            val locations = mutableListOf<Vec3>()
            repeat(movesPerMinute) {
                val prevLoc = ag.location
                if (ag.location dist ag.targetBuilding.coords < 0.00000001) {
                    ag.atBuilding = ag.targetBuilding
                    ag.location = ag.atBuilding.coords
                }
                else ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords).normalize() * ((ag.speed/movesPerMinute).coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat()))
                if(prevLoc != ag.location) locations += ag.location
            }
            if(ag.atBuilding == ag.targetBuilding) needNewBuilding += ag
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