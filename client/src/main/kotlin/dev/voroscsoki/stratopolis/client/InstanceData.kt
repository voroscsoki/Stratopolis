package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import dev.voroscsoki.stratopolis.client.graphics.*
import dev.voroscsoki.stratopolis.common.SimulationData
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
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.time.Clock

class InstanceData(val scene: MainScene) {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        EstablishBearingResponse::class.java to { msg ->
            baselineCoord = (msg as EstablishBearingResponse).baselineCoord
        },
        SimulationResult::class.java to { msg -> runBlocking { setupHeatmap((msg as SimulationResult).data) } },
        RoadResponse::class.java to { msg -> handleRoads(msg as RoadResponse) },
    )

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
    private val roads = ObservableMap<Long, Road>()
    private val agents = ObservableMap<Long, Agent>()
    private val simulationQueue = mutableMapOf<Int, SimulationResult>()
    private var setupJob: Job? = null
    private val coroScope = CoroutineScope(Dispatchers.IO)
    var sequence = 0

    init {
        buildings.listeners.add { change ->
            when (change) {
                is MapChange.Put -> upsertBuilding(change.newVal)
                else -> {}
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


    private fun setupHeatmap(data: SimulationData) {
        //scene.clearHeatmap()
        data.heatmapSquares.forEach {
            println(it)
        }
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

    fun reset(startTime: Instant) {
        currentTime = startTime
        sequence = 0
        agents.clear()
    }
}