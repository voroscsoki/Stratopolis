package dev.voroscsoki.stratopolis.server.osm

import dev.voroscsoki.stratopolis.common.elements.AgeGroup
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import kotlinx.datetime.*
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

    init {
        val count = 10000
        val bldg = DatabaseAccess.getRandomBuildings(count * 2)
        for (i in 0..<count) {
            agents += Agent(Random.nextLong().absoluteValue,
                bldg[i],
                bldg[i + 1],
                bldg[i].coords)
            println(i)
        }
        agents += Agent(1L,
            DatabaseAccess.getBuildingById(776062316L)!!,
            DatabaseAccess.getBuildingById(24726989L)!!,
            DatabaseAccess.getBuildingById(776062316L)!!.coords)
    }

    fun Agent.pickNextBuilding(): Building? {
        val combinedChances = distributions.getDistribution(this.ageGroup, this@Simulation.clock.toLocalDateTime(TimeZone.currentSystemDefault()))
        //pick a random type weighted by the distribution
        // Calculate total weight
        val totalWeight = combinedChances.values.sum()

        // Generate a random value between 0 and total weight
        val randomValue = Random.nextDouble(0.0, totalWeight)

        // Accumulate weights to find the selected building type
        var accumulatedWeight = 0.0
        lateinit var type: String
        for ((buildingType, chance) in combinedChances) {
            accumulatedWeight += chance
            if (randomValue <= accumulatedWeight) {
                type = buildingType
            }
        }
        if(type == this.atBuilding.buildingType) return null
        val buildings = DatabaseAccess.getBuildingsByType(type, this.location)
        if (buildings.isNotEmpty()) {
            this.targetBuilding = buildings.shuffled().first()
            return this.targetBuilding
        }
        return null
    }

    fun tick(callback: (List<Pair<Agent, Agent>>, Instant) -> Unit) {
        clock += 1.minutes
        callback(agents.map { ag ->
            val oldCopy = ag.copy()
            if(ag.id == 1L) println((ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat())))
            ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords).normalize() * (ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat()))
            if (ag.location dist ag.targetBuilding.coords < 0.00000001) {
                ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
                ag.location = ag.atBuilding.coords
            }
            oldCopy to ag.copy()
        }, clock)
    }
}