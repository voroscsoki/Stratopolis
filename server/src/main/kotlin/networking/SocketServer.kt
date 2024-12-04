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
import org.slf4j.LoggerFactory

class SocketServer {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        SimulationRequest::class.java to { msg -> Main.simu.startSimulation((msg as SimulationRequest).starterData) },
        EstablishBearingRequest::class.java to { msg -> runBlocking {
            val res = DatabaseAccess.getAverageCoords()
            sendSocketMessage(EstablishBearingResponse(res)) }},
        BuildingRequest::class.java to { msg -> runBlocking { handleBuildingRequest(msg as BuildingRequest) } },
        RoadRequest::class.java to { msg -> runBlocking { handleRoadRequest(msg as RoadRequest) } },
    )

    private val scope = CoroutineScope(Dispatchers.Default)

    private fun handleBuildingRequest(msg: BuildingRequest) {
        scope.launch {
            sendSocketMessage(BuildingResponse(ResultType.START))
            DatabaseAccess.getBuildings(msg.baseCoord, msg.radius)
                .chunked(40000)
                .forEach { chunk ->
                    sendSocketMessage(BuildingResponse(ResultType.PROGRESS, chunk))
                    Thread.sleep(300)
                }
            sendSocketMessage(BuildingResponse(ResultType.DONE))
        }
    }

    private fun handleRoadRequest(msg: RoadRequest) {
        scope.launch {
            DatabaseAccess.getRoads(msg.baseCoord, msg.radius)
                .chunked(30000)
                .forEach { chunk ->
                    launch {
                        sendSocketMessage(RoadResponse(ResultType.PROGRESS, chunk))
                    }
                }
        }
    }


    val connections = ConcurrentSet<DefaultWebSocketServerSession>()

    fun handleIncomingMessage(msg: ControlMessage) {
        logger.info("Received message: $msg")
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    fun sendSocketMessage(msg: ControlMessage) {
        logger.info("Sending message: $msg")
        CoroutineScope(Dispatchers.IO).launch {
            connections.forEach { it.sendSerialized(msg) }
        }
    }
}