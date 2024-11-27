package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import dev.voroscsoki.stratopolis.client.graphics.*
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.networking.*
import dev.voroscsoki.stratopolis.common.util.MapChange
import dev.voroscsoki.stratopolis.common.util.ObservableMap
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getWayAverage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.toKotlinInstant
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

class InstanceData(val scene: MainScene) {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        EstablishBearingResponse::class.java to { msg -> baselineCoord = (msg as EstablishBearingResponse).baselineCoord },
        AgentStateUpdate::class.java to { msg -> runBlocking { simulationQueue.send(msg as AgentStateUpdate) }},
        SimulationSetupResponse::class.java to { msg -> runBlocking { setupSimulation(msg as SimulationSetupResponse) }},
        RoadResponse::class.java to { msg -> handleRoads(msg as RoadResponse) },
    )

    private fun setupSimulation(msg: SimulationSetupResponse) {
        msg.agents.forEach { a ->
            agents.putIfAbsent(a.id, a)
            coroScope.launch { scene.arrows.putIfAbsent(a.id, scene.createArrow(a.location.toSceneCoords(baselineCoord!!))) }
        }
    }

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
    private val roads = ObservableMap<Long, Road>()
    private val agents = ObservableMap<Long, Agent>()
    private val simulationQueue = Channel<AgentStateUpdate>()
    private var setupJob: Job? = null
    private val coroScope = CoroutineScope(Dispatchers.IO)

    init {
        buildings.listeners.add { change ->
            when (change) {
                is MapChange.Put -> upsertBuilding(change.newVal)
                else -> {}
            }
        }
    }

    private val gameLoopJob = CoroutineScope(Dispatchers.IO).launch {
        val timestep = 0.5
        while(true) {
            val currentUpdate = simulationQueue.receive()
                val newAgents = currentUpdate.agents.map { it.second }.toList()
                while(currentTime < currentUpdate.time) {
                    val timeToCover = currentUpdate.time - currentTime
                    currentTime += timestep.seconds
                    coroutineScope {
                        agents.map { (agentId, agent) ->
                            launch {
                                val currPos = agent.location
                                val targetPos = newAgents.firstOrNull { it.id == agentId }?.location
                                targetPos ?: return@launch

                                val diff = (targetPos - currPos)/((timeToCover.inWholeSeconds.toDouble()/timestep + (1/timestep))).coerceAtLeast(1.0)
                                scene.arrows[agentId]?.location = agent.location.toSceneCoords(baselineCoord!!)
                                agent.location += diff
                            }
                        }
                    }

                    delay((10/timestep).toLong())
                }
        }
    }

    var currentTime = Clock.systemDefaultZone().instant().toKotlinInstant()
    var baselineCoord: Vec3? = null
    private var throttleJob: Job? = null

    private fun throttleRequest(action: () -> Unit) {
        throttleJob?.cancel()
        throttleJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            action()
        }
    }

    init {
        gameLoopJob.start()
    }



    private fun upsertBuilding(data: Building) {
        baselineCoord ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val model = scene.toModel(data, baselineCoord!!) ?: scene.defaultBoxModel
            var inst: ModelInstance? = null
            for (i in 0..3) {
                try {
                    inst = ModelInstance(model)
                    break
                } catch (e: GdxRuntimeException) { //#iterator() cannot be used nested.
                    //TODO: log
                }
            }
            inst ?: return@launch
            val convertedCoords = data.coords.toSceneCoords(baselineCoord!!)
            val validVec =
                Vector3(convertedCoords.x.toFloat(), convertedCoords.y.toFloat(), convertedCoords.z.toFloat())
            inst.transform.setTranslation(validVec)
            inst.materials.forEach { material ->
                material.set(
                    ColorAttribute.createDiffuse(
                        when (data.tags.find { it.key == "building" }?.value) {
                            "commercial" -> Color.BLUE
                            "house" -> Color.GREEN
                            "apartments" -> Color.GREEN
                            "industrial" -> Color.YELLOW
                            "office" -> Color.GRAY
                            "public" -> Color.RED
                            else -> Color.CYAN
                        }
                    )
                )
            }
            scene.putBuilding(convertedCoords, data, inst)
        }
    }

    fun requestBuildings() {
        baselineCoord?.let {
            val source = scene.cam.position?.toWorldCoords(it)!!.copy(y = 0.0)
            runBlocking { Main.socket.sendSocketMessage(BuildingRequest(source, 0.15)) }
        } ?: run {
            Thread.sleep(500)
            requestBuildings()
        }
    }

    fun setupGame() {
        baselineCoord = null
        clearGraphics()
        scene.menu?.loadingBar?.fadeIn()
        setupJob?.cancel()
        runBlocking { Main.socket.sendSocketMessage(EstablishBearingRequest()) }
        runBlocking { requestBuildings() }
        runBlocking { Main.socket.sendSocketMessage(SimulationSetupRequest()) }
    }

    fun clearGraphics() {
        buildings.clear()
        nodes.clear()
        scene.clearGraphicalData()
        scene.cam.position.x = 0f
        scene.cam.position.z = 0f
        scene.cam.update()
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleBuildings(msg: BuildingResponse) {
        msg.buildings.map { buildings.putIfAbsent(it.id, it)}

        throttleRequest {
            scene.updateCaches()
            scene.menu?.loadingBar?.fadeOut()
        }
    }

    private fun handleRoads(msg: RoadResponse) {
        roads.putAll(msg.roads.map { it.id to it })
        msg.roads.forEach { road ->
            CoroutineScope(Dispatchers.IO).launch {
                val model = scene.toModel(road, baselineCoord!!) ?: scene.roadModel
                val inst = ModelInstance(model)
                inst.transform.setTranslation(road.ways.getWayAverage().toSceneCoords(baselineCoord!!).let {
                    Vector3(it.x.toFloat(), -0.05f, it.z.toFloat())
                })
                //scene.putRoad(road.ways.getWayAverage().toSceneCoords(baselineCoord!!), road, inst)
            }

        }
        /*throttleRequest {
            scene.updateCaches()
            scene.menu?.loadingBar?.fadeOut()
        }*/
    }
}