package dev.voroscsoki.stratopolis.server.osm

import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class Simulation {
    var clock = Clock.System.now()
    val agentSpeed = 0.0000001
    val agents = mutableListOf<Agent>()

    init {
        agents += Agent(1L,
            DatabaseAccess.getBuildingById(776062316L)!!,
            DatabaseAccess.getBuildingById(24726989L)!!,
            DatabaseAccess.getBuildingById(776062316L)!!.coords)
    }

    fun tick(callback: (List<Pair<Agent, Agent>>, Instant) -> Unit) {
        clock += 1.minutes
        callback(agents.map { ag ->
            val oldCopy = ag.copy()
            ag.location += (ag.targetBuilding.coords - ag.atBuilding.coords) * (1 / 3f)
            if (ag.location dist ag.targetBuilding.coords < 0.0001) {
                ag.atBuilding = ag.targetBuilding.also { ag.targetBuilding = ag.atBuilding }
            }
            oldCopy to ag.copy()
        }, clock)
    }
}