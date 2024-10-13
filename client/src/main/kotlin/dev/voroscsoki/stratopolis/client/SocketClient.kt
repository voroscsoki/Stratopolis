package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage.ClientMessage
import dev.voroscsoki.stratopolis.common.api.controlMessageModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SocketClient {
    private val client = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json {
                serializersModule = controlMessageModule
                classDiscriminator = "type"
                prettyPrint = true
            })
        }
    }
    suspend fun testEcho() {
        client.webSocket("ws://localhost:8085/transfertest") {
            sendSerialized(ClientMessage.EchoRequestEvent)
        }
    }
}
