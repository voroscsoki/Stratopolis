package dev.voroscsoki.stratopolis.server.networking

import dev.voroscsoki.stratopolis.common.networking.*
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import dev.voroscsoki.stratopolis.server.Main
import io.ktor.server.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SocketServer {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        TickRequest::class.java to { Main.simu.tick { agents, time -> Main.socketServer.sendSocketMessage(AgentStateUpdate(agents, time)) } },
        SimulationSetupRequest::class.java to { handleSimuSetup() },
        EstablishBearingRequest::class.java to { msg -> runBlocking {
            val res = DatabaseAccess.getAverageCoords()
            sendSocketMessage(EstablishBearingResponse(ControlResult.OK, res)) }},
        BuildingRequest::class.java to { msg -> runBlocking { handleBuildingRequest(msg as BuildingRequest) } },
        RoadRequest::class.java to { msg -> runBlocking { handleRoadRequest(msg as RoadRequest) } },
    )

    private fun handleSimuSetup() {
        scope.launch {
            Main.simu.agents.chunked(1000).forEach {
                Main.socketServer.sendSocketMessage(SimulationSetupResponse(it))
            }
        }

    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun handleBuildingRequest(msg: BuildingRequest) {
        scope.launch {
            DatabaseAccess.getBuildings(msg.baseCoord, msg.radius)
                .chunked(30000)
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
                .chunked(30000)
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

    fun sendSocketMessage(msg: ControlMessage) {
        println("Sending message: $msg")
        CoroutineScope(Dispatchers.IO).launch {
            connections.forEach { it.sendSerialized(msg) }
        }
    }
}