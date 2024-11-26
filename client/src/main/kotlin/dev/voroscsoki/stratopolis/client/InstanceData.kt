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
        AgentStateUpdate::class.java to { msg -> runBlocking { handleAgentUpdate((msg as AgentStateUpdate)) }},
        RoadResponse::class.java to { msg -> handleRoads(msg as RoadResponse) },
    )

    private suspend fun handleAgentUpdate(it: AgentStateUpdate) {
        it.agents.map { a -> agents.putIfAbsent(a.first.id, a.first)}
        simulationQueue.send(it)
    }

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
    private val roads = ObservableMap<Long, Road>()
    private val agents = ObservableMap<Long, Agent>()
    private val simulationQueue = Channel<AgentStateUpdate>()
    private val gameLoopJob = CoroutineScope(Dispatchers.IO).launch {
        val timestep = 0.5
        while(true) {
            val currentUpdate = simulationQueue.receive()
                val newAgents = currentUpdate.agents.map { it.second }.toList()
                while(currentTime < currentUpdate.time) {
                    val timeToCover = currentUpdate.time - currentTime
                    currentTime += timestep.seconds
                    for (agent in agents) {
                        val currPos = agent.value.location
                        val targetPos = newAgents.firstOrNull { it.id == agent.key }?.location
                        targetPos ?: continue

                        val diff = (targetPos - currPos)/((timeToCover.inWholeSeconds.toDouble()/timestep + (1/timestep))).coerceAtLeast(1.0)
                        agent.value.location += diff
                        scene.arrows.getOrPut(agent.key) { scene.createArrow() }.location = agents[agent.key]!!.location.toSceneCoords(baselineCoord!!)
                    }
                    Thread.sleep((10/timestep).toLong())
                }
        }
    }

    var currentTime = Clock.systemDefaultZone().instant().toKotlinInstant()
    var baselineCoord: Vec3? = null
    private var throttleJob: Job? = null

    private fun throttleRequest(action: () -> Unit) {
        throttleJob?.cancel() // Cancel any previous timer
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            delay(4000) // Wait for the specified delay
            action() // Execute the action if no further calls reset the timer
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
        baselineCoord ?: return
        val source = scene.cam.position?.toWorldCoords(baselineCoord!!)!!.copy(y = 0.0)
        runBlocking {
            if(Main.socket.sendSocketMessage(BuildingRequest(source, 0.15)))
                scene.menu?.loadingBar?.fadeIn()
        }
    }

    suspend fun setupGame() {
        baselineCoord = null
        clearGraphics()
        if(Main.socket.sendSocketMessage(EstablishBearingRequest())) {
            for(i in 0..<100) {
                if (baselineCoord != null){
                    requestBuildings()
                    break
                }
                delay(1000)
            }
        }
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
        buildings.putAll(msg.buildings.map { it.id to it })
        msg.buildings.forEach { upsertBuilding(it) }

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
                    Vector3(it.x.toFloat(), -5f, it.z.toFloat())
                })
                scene.putRoad(road.ways.getWayAverage().toSceneCoords(baselineCoord!!), road, inst)
            }

        }
        throttleRequest {
            scene.updateCaches()
            scene.menu?.loadingBar?.fadeOut()
        }
    }
}