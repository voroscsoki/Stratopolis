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
    private var setupJob: Job? = null
    var graphicsLoading: Boolean = false
        set(value) = run {
            if(value) {
                scene.menu?.loadingBar?.fadeIn()
            } else {
                scene.menu?.loadingBar?.fadeOut()
            }
            scene.menu?.disableButtons(value)
            field = value
        }
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
        data.frequencies.forEach { freq ->
            val vec = freq.key.split(",").let { Vec3(it[0].toDouble(), 0.0, it[1].toDouble()) }
                .toSceneCoords(baselineCoord!!).roundToNearestInt()
            freq.value.keys.map {
                scene.heatmaps[it]?.updateFrequency(vec, freq.value[it]!!)
                scene.heatmaps[null]?.updateFrequency(vec, freq.value.values.sum())
            }
        }
        graphicsLoading = false
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
                        when (data.buildingType) {
                            "commercial" -> Color.BLUE
                            "retail" -> Color.BLUE
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
            graphicsLoading = true
            runBlocking { Main.socket.sendSocketMessage(BuildingRequest(source, 0.3)) }
        } ?: run {
            Thread.sleep(500)
            requestBuildings()
        }
    }

    fun setupGame() {
        baselineCoord = null
        setupJob?.cancel()
        runBlocking { if(!Main.socket.isWebSocketAvailable(Main.socket.targetAddress)) return@runBlocking
            graphicsLoading = true
            Main.socket.sendSocketMessage(EstablishBearingRequest())
            requestBuildings()
        }
    }

    fun clearGraphics() {
        buildings.clear()
        nodes.clear()
        scene.clearGraphicalData()
        scene.clearHeatmap()
        scene.cam.position.x = 0f
        scene.cam.position.z = 0f
        scene.cam.update()
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleBuildings(msg: BuildingResponse) {
        if(msg.res == ResultType.START) clearGraphics()
        msg.buildings.map { buildings.putIfAbsent(it.id, it)}
        println("Buildings: ${buildings.size}")
        if(msg.res == ResultType.DONE) {
            scene.updateCaches()
            graphicsLoading = false
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