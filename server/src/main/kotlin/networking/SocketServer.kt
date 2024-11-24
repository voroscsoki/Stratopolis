package dev.voroscsoki.stratopolis.server.networking

import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.networking.*
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import dev.voroscsoki.stratopolis.server.Main.Companion.simu
import dev.voroscsoki.stratopolis.server.Main.Companion.socketServer
import io.ktor.server.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant

class SocketServer {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        BuildingRequest::class.java to { msg -> runBlocking { handleBuildingRequest(msg as BuildingRequest) } },
        SimulationStartRequest::class.java to { simu.tick { agents: List<Pair<Agent, Agent>>, time: Instant -> socketServer.sendSocketMessage(AgentStateUpdate(agents, time)) } },
        EstablishBearingRequest::class.java to { msg -> runBlocking { sendSocketMessage(EstablishBearingResponse(ControlResult.OK, DatabaseAccess.getAverageCoords())) }},
        RoadRequest::class.java to { msg -> runBlocking { handleRoadRequest(msg as RoadRequest) } },
    )
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun handleBuildingRequest(msg: BuildingRequest) {
        scope.launch {
            DatabaseAccess.getBuildings(msg.baseCoord, msg.radius)
                .chunked(20000)
                .forEach { chunk ->
                    launch {
                        sendSocketMessage(BuildingResponse(ControlResult.OK, chunk))
                    }
                }
        }
    }

    private fun handleRoadRequest(msg: RoadRequest) {
        scope.launch {
            DatabaseAccess.getRoads(msg.baseCoord, msg.radius)
                .chunked(20000)
                .forEach { chunk ->
                    launch {
                        sendSocketMessage(RoadResponse(ControlResult.OK, chunk))
                    }
                }
        }
    }


    val connections = ConcurrentSet<DefaultWebSocketServerSession>()

    fun handleIncomingMessage(msg: ControlMessage) {
        println("Received message: $msg")
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun sendSocketMessage(msg: ControlMessage) {
        println("Sending message: $msg")
        CoroutineScope(Dispatchers.IO).launch {
            connections.forEach { it.sendSerialized(msg) }
        }
    }
}