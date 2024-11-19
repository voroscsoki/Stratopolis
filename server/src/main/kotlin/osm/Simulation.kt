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
        val bldg1 = DatabaseAccess.getBuildingById(776062316)
        val bldg2 = DatabaseAccess.getBuildingById(82478999)
        val bldg3 = DatabaseAccess.getBuildingById(32800779)
        val all = DatabaseAccess.getBuildings().toList()
        agents += Agent(1, bldg1!!, bldg2!!, bldg1.coords)
        agents += Agent(2, bldg1!!, bldg3!!, bldg1.coords)
        repeat(10) {
            agents += Agent(it + 3L, bldg1!!, all[Random.nextInt(all.size)], bldg1.coords)
        }
    }

    fun tick(callback: (List<Agent>, Instant) -> Unit) {
        clock += 1.minutes
        agents.forEach { ag ->
            ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords) * (1 / 10f)
            if (ag.location dist ag.targetBuilding.coords < 0.0001) {
                ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
            }
        }
        callback(agents, clock)
    }
}