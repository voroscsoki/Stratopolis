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
class NodeRequest(val baseCoord: Vec3?) : ControlMessage()

@Serializable
class NodeResponse(val res: ControlResult, val nodes: List<SerializableNode> = emptyList()) : ControlMessage()

@Serializable
class BuildingRequest(val baseCoord: Vec3?, val radius: Vec3? = null) : ControlMessage()

@Serializable
class BuildingResponse(val res: ControlResult, val buildings: List<Building> = emptyList()) : ControlMessage()

@Serializable
class SimulationStartRequest : ControlMessage()

@Serializable
class AgentStateUpdate(val agent: Agent) : ControlMessage()