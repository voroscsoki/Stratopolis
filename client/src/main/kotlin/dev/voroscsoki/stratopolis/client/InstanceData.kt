package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.networking.AgentStateUpdate
import dev.voroscsoki.stratopolis.common.networking.BuildingResponse
import dev.voroscsoki.stratopolis.common.networking.ControlMessage
import dev.voroscsoki.stratopolis.common.networking.NodeResponse
import dev.voroscsoki.stratopolis.common.util.MapChange
import dev.voroscsoki.stratopolis.common.util.ObservableMap
import kotlinx.coroutines.*

class InstanceData {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) },
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        AgentStateUpdate::class.java to { msg -> runBlocking { (msg as AgentStateUpdate).let { Main.appScene.moveAgents(it.agents, it.time) } }}
    )

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
    private var throttleJob: Job? = null

    private fun throttleRequest(action: () -> Unit) {
        throttleJob?.cancel() // Cancel any previous timer
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            delay(500) // Wait for the specified delay
            action() // Execute the action if no further calls reset the timer
        }
    }

    init {
        /*nodes.listeners.add { change ->
            when (change) {
                is MapChange.Put -> Main.appScene.upsertNode(change.newVal)
                else -> {}
            }
        }*/

        buildings.listeners.add { change ->
            when (change) {
                is MapChange.Put ->  Main.appScene.upsertBuilding(change.newVal)
                else -> {}
            }
        }
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleBuildings(msg: BuildingResponse) {
        buildings.putAll(msg.buildings.map { it.id to it })
        throttleRequest { Main.appScene.updateCaches() }
    }

    private fun handleNodes(msg: NodeResponse) {
        msg.nodes.forEach { nodes += it.id to it }
    }


}