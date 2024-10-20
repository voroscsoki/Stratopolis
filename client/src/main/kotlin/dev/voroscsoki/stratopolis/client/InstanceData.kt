package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.HttpResponse

class InstanceData {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        HttpResponse::class.java to { msg -> handleHttp(msg as HttpResponse) }
    )


    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleHttp(msg: HttpResponse) {
        println("HTTP response: ${msg.code} - ${msg.message}")
    }
}