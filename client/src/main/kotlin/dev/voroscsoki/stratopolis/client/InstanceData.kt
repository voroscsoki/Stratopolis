package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.HttpResponse
import dev.voroscsoki.stratopolis.common.api.NodeResponse

class InstanceData {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        HttpResponse::class.java to { msg -> handleHttp(msg as HttpResponse) },
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) }
    )


    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleHttp(msg: HttpResponse) {
        println("HTTP response: ${msg.code} - ${msg.message}")
    }

    private fun handleNodes(msg: NodeResponse) {
        println("Building response: ${msg.res} - ${msg.nodes.count()}")
    }
}