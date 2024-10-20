package dev.voroscsoki.stratopolis.common.api
import kotlinx.serialization.Serializable

@Serializable
sealed class ControlMessage

@Serializable
class OsmLoadRequest(val path: String) : ControlMessage()

@Serializable
class HttpResponse(val code: Int, val message: String) : ControlMessage()
