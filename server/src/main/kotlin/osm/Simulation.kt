package dev.voroscsoki.stratopolis.server.osm

import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class Simulation {
    var clock = Clock.System.now()
    val agentSpeed = 0.0000001
    val agents = mutableListOf<Agent>()

    init {
        val bldg = DatabaseAccess.getRandomBuildings(2000)
        for (i in 0..1000) {
            agents += Agent(Random.nextLong(),
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

    fun tick(callback: (List<Pair<Agent, Agent>>, Instant) -> Unit) {
        clock += 1.minutes
        callback(agents.map { ag ->
            val oldCopy = ag.copy()
            if(ag.id == 1L) println((ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat())))
            ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords).normalize() * (ag.speed.coerceAtMost((ag.targetBuilding.coords dist ag.location).toFloat()))
            if(ag.id == 1L) {
                println("${ag.atBuilding.coords dist ag.location} -> ${ag.targetBuilding.coords dist ag.location} -> ${ag.location}")
            }
            if (ag.location dist ag.targetBuilding.coords < 0.00000001) {
                ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
                ag.location = ag.atBuilding.coords
            }
            oldCopy to ag.copy()
        }, clock)
    }
}