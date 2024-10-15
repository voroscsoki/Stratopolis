package dev.voroscsoki.stratopolis.common.api

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend fun WebSocketSession.sendSerialized(msg: ControlMessage) {
    val json = Json.encodeToString(msg)
    send(json)
}