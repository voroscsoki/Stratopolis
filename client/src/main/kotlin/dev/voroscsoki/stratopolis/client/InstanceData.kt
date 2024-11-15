package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.networking.AgentStateUpdate
import dev.voroscsoki.stratopolis.common.networking.BuildingResponse
import dev.voroscsoki.stratopolis.common.networking.ControlMessage
import dev.voroscsoki.stratopolis.common.networking.NodeResponse
import dev.voroscsoki.stratopolis.common.util.MapChange
import dev.voroscsoki.stratopolis.common.util.ObservableMap
import kotlinx.coroutines.runBlocking

class InstanceData {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) },
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        AgentStateUpdate::class.java to { msg -> runBlocking { (msg as AgentStateUpdate).let { Main.appScene.moveAgents(it.agents, it.time) } }}
    )

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()

    init {
        /*nodes.listeners.add { change ->
            when (change) {
                is MapChange.Put -> Main.appScene.upsertNode(change.newVal)
                else -> {}
            }
        }*/

        buildings.listeners.add { change ->
            when (change) {
                is MapChange.Put -> Main.appScene.upsertBuilding(change.newVal)
                else -> {}
            }
        }
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleBuildings(msg: BuildingResponse) {
        msg.buildings.forEach { buildings += it.id to it }
    }

    private fun handleNodes(msg: NodeResponse) {
        msg.nodes.forEach { nodes += it.id to it }
    }


}