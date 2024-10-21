package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.HttpResponse
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
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    routing {
        get("/health-check") {
            call.respond("OK")
        }


        webSocket("/control") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val msg = try { Json.decodeFromString<ControlMessage>(frame.readText()) } catch (e: SerializationException) { null }
                    println(msg)
                    msg?.let { sendSerialized(HttpResponse(200, "OK")) }
                }
            }
        }
    }
}