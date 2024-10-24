package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.*
import io.ktor.server.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.runBlocking

class SocketServer {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeRequest::class.java to { msg -> runBlocking { handleNodeRequest(msg as NodeRequest) }}
    )

    private suspend fun handleNodeRequest(msg: NodeRequest) {
        val res = DatabaseAccess.getNodes(msg.baseCoord)
        val chunked = res.chunked(1000)
        chunked.forEach { sendSocketMessage(NodeResponse(ControlResult.OK, it)) }
    }


    val connections = ConcurrentSet<DefaultWebSocketServerSession>()

    fun handleIncomingMessage(msg: ControlMessage) {
        println("Received message: $msg")
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    suspend fun sendSocketMessage(msg: ControlMessage) {
        println("Sending message: $msg")
        connections.forEach { it.sendSerialized(msg) }
    }
}