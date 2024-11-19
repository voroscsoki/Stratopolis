package dev.voroscsoki.stratopolis.server.networking

import dev.voroscsoki.stratopolis.common.networking.ControlMessage
import dev.voroscsoki.stratopolis.server.Main
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerializationException
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
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        })
    }

    routing {
        get("/health-check") {
            call.respond("OK")
        }


        webSocket("/control") {
            Main.socketServer.connections += this
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val msg = try { Json.decodeFromString<ControlMessage>(frame.readText()) } catch (e: SerializationException) { null }
                        msg?.let { Main.socketServer.handleIncomingMessage(it) }
                    }
                }
            }
            finally {
                Main.socketServer.connections -= this
            }
        }
    }
}