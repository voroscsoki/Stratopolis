package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.GdxRuntimeException
import dev.voroscsoki.stratopolis.client.graphics.MainScene
import dev.voroscsoki.stratopolis.client.graphics.fadeIn
import dev.voroscsoki.stratopolis.client.graphics.toSceneCoords
import dev.voroscsoki.stratopolis.client.graphics.toWorldCoords
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.networking.*
import dev.voroscsoki.stratopolis.common.util.MapChange
import dev.voroscsoki.stratopolis.common.util.ObservableMap
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.toKotlinInstant
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

class InstanceData(val scene: MainScene) {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) },
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        EstablishBearingResponse::class.java to { msg -> baselineCoord = (msg as EstablishBearingResponse).baselineCoord },
        AgentStateUpdate::class.java to { msg -> runBlocking { handleAgentUpdate((msg as AgentStateUpdate)) }}
    )

    private suspend fun handleAgentUpdate(it: AgentStateUpdate) {
        it.agents.map { a -> agents.putIfAbsent(a.first.id, a.first)}
        simulationQueue.send(it)
    }

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
    private val agents = ObservableMap<Long, Agent>()
    private val simulationQueue = Channel<AgentStateUpdate>()
    private val gameLoopJob = CoroutineScope(Dispatchers.IO).launch {
        while(true) {
            val currentUpdate = simulationQueue.receive()
                val newAgents = currentUpdate.agents.map { it.second }.toList()
                while(currentTime < currentUpdate.time) {
                    val timeToCover = currentUpdate.time - currentTime
                    currentTime += 1.seconds
                    for (agent in agents) {
                        val currPos = agent.value.location
                        val targetPos = newAgents.firstOrNull { it.id == agent.key }?.location
                        targetPos ?: continue

                        val diff = (targetPos - currPos)/(timeToCover.inWholeSeconds.toFloat() + 1f).coerceAtLeast(1f)
                        agent.value.location += diff
                        scene.arrows.getOrPut(agent.key) { scene.createArrow() }.location = agents[agent.key]!!.location.toSceneCoords(baselineCoord!!)
                        println(diff)
                    }
                    Thread.sleep(30)
                }
        }
    }

    var currentTime = Clock.systemDefaultZone().instant().toKotlinInstant()
    private var baselineCoord: Vec3? = null
    private var throttleJob: Job? = null

    private fun throttleRequest(action: () -> Unit) {
        throttleJob?.cancel() // Cancel any previous timer
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            delay(2000) // Wait for the specified delay
            action() // Execute the action if no further calls reset the timer
        }
    }

    init {
        buildings.listeners.add { change ->
            when (change) {
                is MapChange.Put ->  upsertBuilding(change.newVal)
                else -> {}
            }
        }
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
            scene.putBuilding(convertedCoords, data, model, inst)
        }
    }

    fun requestBuildings() {
        baselineCoord ?: return
        val source = scene.cam.position?.toWorldCoords(baselineCoord!!)!!.copy(y = 0.0)
        runBlocking {
            if(Main.socket.sendSocketMessage(BuildingRequest(source, 0.05)))
                scene.menu?.loadingBar?.fadeIn()
        }
    }

    suspend fun setupGame() {
        baselineCoord = null
        clearGraphics()
        if(Main.socket.sendSocketMessage(EstablishBearingRequest())) {
            for(i in 0..<10) {
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
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleBuildings(msg: BuildingResponse) {
        buildings.putAll(msg.buildings.map { it.id to it })
        println(buildings.size)

        throttleRequest { scene.updateCaches() }
    }

    private fun handleNodes(msg: NodeResponse) {
        msg.nodes.forEach { nodes += it.id to it }
    }
}