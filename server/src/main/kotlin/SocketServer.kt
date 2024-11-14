package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.*
import dev.voroscsoki.stratopolis.server.Main.Companion.simu
import dev.voroscsoki.stratopolis.server.Main.Companion.socketServer
import io.ktor.server.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SocketServer {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeRequest::class.java to { msg -> runBlocking { handleNodeRequest(msg as NodeRequest) } },
        BuildingRequest::class.java to { msg -> runBlocking { handleBuildingRequest(msg as BuildingRequest) } },
        SimulationStartRequest::class.java to { simu.tick { agent: Agent -> socketServer.sendSocketMessage(AgentStateUpdate(agent)) } }
    )
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun handleNodeRequest(msg: NodeRequest) {
        scope.launch {
            DatabaseAccess.getNodes(msg.baseCoord)
                .chunked(1000)
                .forEach { chunk ->
                    launch {
                        sendSocketMessage(NodeResponse(ControlResult.OK, chunk))
                    }
                }
        }
    }

    private fun handleBuildingRequest(msg: BuildingRequest) {
        scope.launch {
            DatabaseAccess.getBuildings(msg.baseCoord)
                .chunked(1000)
                .forEach { chunk ->
                    launch {
                        sendSocketMessage(BuildingResponse(ControlResult.OK, chunk))
                    }
                }
        }
    }


    val connections = ConcurrentSet<DefaultWebSocketServerSession>()

    fun handleIncomingMessage(msg: ControlMessage) {
        println("Received message: $msg")
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    fun sendSocketMessage(msg: ControlMessage) {
        println("Sending message: $msg")
        CoroutineScope(Dispatchers.IO).launch {
            connections.forEach { it.sendSerialized(msg) }
        }
    }
}