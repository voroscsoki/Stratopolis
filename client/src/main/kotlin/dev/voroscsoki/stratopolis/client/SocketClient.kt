package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.sendSerialized
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SocketClient {
    private val client: HttpClient = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private var msgHandler = { msg: ControlMessage -> Main.instanceData.handleIncomingMessage(msg) }
    private val sendQueue = Channel<ControlMessage>()

    suspend fun activateSocket() {
        client.webSocket("ws://localhost:8085/control") {
            coroutineScope {
                //parallel sending and receiving
                launch {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val msg = try { Json.decodeFromString<ControlMessage>(frame.readText()) } catch (e: SerializationException) { null }
                            println(msg)
                            msg?.let { msgHandler.invoke(it) }
                        }
                    }
                }
                launch {
                    for (item in sendQueue) {
                        sendSerialized(item)
                    }
                }
            }
        }
    }
    suspend fun sendSocketMessage(msg: ControlMessage) {
        sendQueue.send(msg)
    }
}
