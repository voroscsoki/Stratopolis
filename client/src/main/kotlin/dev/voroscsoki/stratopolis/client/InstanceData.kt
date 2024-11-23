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
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.networking.*
import dev.voroscsoki.stratopolis.common.util.MapChange
import dev.voroscsoki.stratopolis.common.util.ObservableMap
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.coroutines.*

class InstanceData(val scene: MainScene) {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) },
        BuildingResponse::class.java to { msg -> handleBuildings(msg as BuildingResponse) },
        EstablishBearingResponse::class.java to { msg -> baselineCoord = (msg as EstablishBearingResponse).baselineCoord },
        //AgentStateUpdate::class.java to { msg -> runBlocking { (msg as AgentStateUpdate).let { Main.appScene.moveAgents(it.agents, it.time) } }}
    )

    private val nodes = ObservableMap<Long, SerializableNode>()
    private val buildings = ObservableMap<Long, Building>()
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
                Vector3(convertedCoords.x, convertedCoords.y, convertedCoords.z)
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
        val source = scene.cam.position?.toWorldCoords(baselineCoord!!)!!.copy(y = 0f)
        runBlocking {
            if(Main.socket.sendSocketMessage(BuildingRequest(source, 0.05f)))
                scene.menu?.loadingBar?.fadeIn()
        }
    }

    suspend fun setupGame() {
        baselineCoord = null
        clear()
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

    fun clear() {
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