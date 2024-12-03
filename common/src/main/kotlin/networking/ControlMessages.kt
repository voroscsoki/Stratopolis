package dev.voroscsoki.stratopolis.common.networking
import dev.voroscsoki.stratopolis.common.SimulationData
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.serialization.Serializable

enum class ResultType {
    START,
    PROGRESS,
    DONE,
}

@Serializable
sealed class ControlMessage

@Serializable
class OsmLoadRequest(val file: ByteArray) : ControlMessage()

@Serializable
class BuildingRequest(val baseCoord: Vec3?, val radius: Double? = null) : ControlMessage()

@Serializable
class BuildingResponse(val res: ResultType, val buildings: List<Building> = emptyList()) : ControlMessage()

@Serializable
class RoadRequest(val baseCoord: Vec3?, val radius: Double? = null) : ControlMessage()

@Serializable
class RoadResponse(val res: ResultType, val roads: List<Road> = emptyList()) : ControlMessage()

@Serializable
class SimulationRequest(val starterData: SimulationData) : ControlMessage()

@Serializable
class SimulationResult(val data: SimulationData) : ControlMessage()

@Serializable
class EstablishBearingRequest : ControlMessage()

@Serializable
class EstablishBearingResponse(val baselineCoord: Vec3) : ControlMessage()