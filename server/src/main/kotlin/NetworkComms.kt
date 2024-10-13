package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.ControlMessage
import dev.voroscsoki.stratopolis.common.api.controlMessageModule
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = controlMessageModule
            classDiscriminator = "type"
            prettyPrint = true
        })
    }
    install(WebSockets)

    routing {
        staticResources("static", "static")
        // Define a route at /test
        //send the json representation of the Building object
        get("/test") {
            val bldg = OsmStorage.buildings.take(10000)
            call.respond(bldg)
        }

        get("/health-check") {
            call.respond("OK")
        }

        webSocket("/echo") {
            send("Please enter your name")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                if (receivedText.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                } else {
                    send(Frame.Text("Hi, $receivedText!"))
                }
            }
        }
        webSocket("/transfertest") {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val response = receiveDeserialized<ControlMessage>()
                    println("Received response: $response")
                }
            }
        }
    }
}