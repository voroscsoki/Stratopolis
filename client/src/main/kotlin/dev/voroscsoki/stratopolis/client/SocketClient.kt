package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.sendSerialized
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SocketClient {
    private val client: HttpClient = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private var ws: DefaultClientWebSocketSession? = null
    private var msgHandler = { msg: ControlMessage -> Main.instanceData.handleIncomingMessage(msg) }

    suspend fun listenOnSocket() {
        client.webSocket("ws://localhost:8085/control") {
            launch {
                for (frame in ws!!.incoming) {
                    if (frame is Frame.Text) {
                        val msg = try { Json.decodeFromString<ControlMessage>(frame.readText()) } catch (e: SerializationException) { null }
                        msg?.let { msgHandler.invoke(it) }
                    }
                }
            }
        }

    }
    suspend fun sendSocketMessage(msg: ControlMessage) {
        client.webSocket("ws://localhost:8085/control") {
            sendSerialized(msg)
        }
    }
}
