package dev.voroscsoki.stratopolis.common.networking
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
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
class OsmLoadRequest(val file: ByteArray) : ControlMessage()

@Serializable
class BuildingRequest(val baseCoord: Vec3?, val radius: Double? = null) : ControlMessage()

@Serializable
class BuildingResponse(val res: ControlResult, val buildings: List<Building> = emptyList()) : ControlMessage()

@Serializable
class RoadRequest(val baseCoord: Vec3?, val radius: Double? = null) : ControlMessage()

@Serializable
class RoadResponse(val res: ControlResult, val roads: List<Road> = emptyList()) : ControlMessage()

@Serializable
data object SimulationStartRequest : ControlMessage()

@Serializable
class AgentStateUpdate(val agents: List<Pair<Agent, Agent>>, val time: Instant) : ControlMessage()

@Serializable
class EstablishBearingRequest : ControlMessage()

@Serializable
class EstablishBearingResponse(val res: ControlResult, val baselineCoord: Vec3) : ControlMessage()