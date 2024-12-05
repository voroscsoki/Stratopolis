package dev.voroscsoki.stratopolis.server.networking

import dev.voroscsoki.stratopolis.common.networking.ControlMessage
import dev.voroscsoki.stratopolis.server.DatabaseAccess
import dev.voroscsoki.stratopolis.server.Main
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = 10.seconds.toJavaDuration()
        timeout = 240.seconds.toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        })
    }

    routing {
        post("/pbf_file") {
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val byteArrayStream = ByteArrayOutputStream()
                        part.streamProvider().use { input ->
                            input.copyTo(byteArrayStream)
                        }
                        fileBytes = byteArrayStream.toByteArray()
                    }
                    else -> Unit
                }
                part.dispose()
            }

            runBlocking {
                if (fileBytes != null) {
                    DatabaseAccess.reinitalizeDB(fileBytes!!)
                }
                call.respond(HttpStatusCode.OK)
            }
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