package dev.voroscsoki.stratopolis.common.networking
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.datetime.Instant
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
class BuildingRequest(val baseCoord: Vec3?, val radius: Float? = null) : ControlMessage()

@Serializable
class BuildingResponse(val res: ControlResult, val buildings: List<Building> = emptyList()) : ControlMessage()

@Serializable
class SimulationStartRequest : ControlMessage()

@Serializable
class AgentStateUpdate(val agents: List<Agent>, val time: Instant) : ControlMessage()

@Serializable
class EstablishBearingRequest() : ControlMessage()

@Serializable
class EstablishBearingResponse(val res: ControlResult, val baselineCoord: Vec3) : ControlMessage()