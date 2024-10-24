package dev.voroscsoki.stratopolis.common.api
import kotlinx.serialization.Serializable

enum class ControlResult {
    OK,
    WARN,
    ERROR
}

@Serializable
sealed class ControlMessage

@Serializable
class OsmLoadRequest(val path: String) : ControlMessage()

@Serializable
class HttpResponse(val code: Int, val message: String) : ControlMessage()

@Serializable
class NodeRequest(val baseCoord: Vec3?) : ControlMessage()

@Serializable
class NodeResponse(val res: ControlResult, val nodes: List<SerializableNode> = emptyList()) : ControlMessage()