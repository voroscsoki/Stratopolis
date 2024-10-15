package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.EchoReq
import dev.voroscsoki.stratopolis.common.api.EchoResp
import dev.voroscsoki.stratopolis.common.api.sendSerialized
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

class SocketClient {
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    suspend fun testEcho() {
        client.webSocket("ws://localhost:8085/echo") {
            sendSerialized(EchoReq("Hello from the client!"))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val response = Json.decodeFromString<ControlMessage>(frame.readText())
                    if (response is EchoResp) {
                        println("Received: ${response.msg}")
                        close()
                    }
                }
            }
        }
    }


}
