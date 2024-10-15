package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.EchoReq
import dev.voroscsoki.stratopolis.common.api.EchoResp
import dev.voroscsoki.stratopolis.common.api.sendSerialized
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = 1.minutes.toJavaDuration()
        timeout = 15.seconds.toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    routing {
        webSocket("/echo") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val msg = Json.decodeFromString<ControlMessage>(frame.readText())
                    if(msg is EchoReq) {
                        sendSerialized(EchoResp("Echoing back: ${msg.msg}"))
                    }
                }
            }
        }
    }
}