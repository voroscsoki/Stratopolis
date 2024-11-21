package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ShortArray
import com.badlogic.gdx.utils.viewport.ScreenViewport
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.user_interface.UtilInput
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.networking.BuildingRequest
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class GraphicalBuilding(val apiData: Building?, val model: Model, val instance: ModelInstance)

data class CacheObject(val cache: ModelCache, val lock: Mutex, val startingCoords: Vector3, val size: Int, var isVisible: Boolean = false) {
    fun checkVisibility(cam: PerspectiveCamera) {
        val resolution = 32
        for (x in 0..resolution) {
            for (z in 0..resolution) {
                val point = Vector3(
                    startingCoords.x + (x.toFloat()/resolution) * size,
                    startingCoords.y,
                    startingCoords.z + (z.toFloat()/resolution) * size
                )
                if (cam.frustum.pointInFrustum(point)) {
                    isVisible = true
                    return
                }
            }
        }
        isVisible = false
    }
}

class MainScene : ApplicationListener {
    //libGDX variables
    lateinit var cam: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var defaultBoxModel: Model
    private lateinit var arrowModel: Model
    private lateinit var environment: Environment

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private var popup: PopupWindow? = null
    private var menu: GameMenu? = null

    //constants
    private val chunkSize = 5000
    val baselineCoord = Vec3(47.472935f, 0f, 19.053410f)

    //updatables
    private val chunks = ConcurrentHashMap<String, ConcurrentHashMap<Long, GraphicalBuilding>>()
    private val caches = ConcurrentHashMap<String, CacheObject>()
    private val agents = ConcurrentHashMap<Long, Agent>()
    private val arrows = ConcurrentHashMap<Long, ModelInstance>()

    //helper
    private var keyframeCounter = 0

    // text variables
    private var currentTime: Instant = Instant.fromEpochSeconds(0)
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun create() {
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

        modelBatch = ModelBatch(DefaultShaderProvider()) { _, _ -> /*No sorting*/ }
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 100f, 0f)
            lookAt(0f, 0f, 0f)
            up.set(1f, 0f, 0f)
            near = 1f
            far = 20000f
            update()
        }

        stage = Stage(ScreenViewport())
        skin = createDefaultGdxSkin()

        modelBuilder = ModelBuilder()
        defaultBoxModel = modelBuilder.createBox(
            1.6f, 0.8f, 1.6f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        arrowModel = modelBuilder.createArrow(
            Vector3(0f, 100f, 0f),
            Vector3(20f, 100f, 0f),
            Material(ColorAttribute.createDiffuse(Color.RED)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        val inst = ModelInstance(defaultBoxModel)
        //inst.transform.setTranslation(Vector3(0.4f, 0f, 0.4f))
        chunks.getOrPut(getChunkKey(0f, 0f)) { ConcurrentHashMap() }[0] = GraphicalBuilding(null, defaultBoxModel, inst)

        val multiplexer = InputMultiplexer().apply {
            addProcessor(CustomCameraController(cam))
            addProcessor(UtilInput(this@MainScene))
            addProcessor(stage)
        }
        Gdx.input.inputProcessor = multiplexer
        Gdx.gl.glEnable(GL40.GL_CULL_FACE)
        Gdx.gl.glCullFace(GL40.GL_BACK)


        // Initialize FPS counter
        spriteBatch = SpriteBatch()
        font = BitmapFont()
        this.showMenu()
        requestBuildings()
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL40.GL_COLOR_BUFFER_BIT or GL40.GL_DEPTH_BUFFER_BIT)

        val isKeyframe = (keyframeCounter++ == 5).also {
            if(it) keyframeCounter = 0
        }

        modelBatch.begin(cam)
        if(caches.isNotEmpty()) {
            caches.values.forEach {
                if (it.lock.isLocked) return@forEach
                if (isKeyframe) it.checkVisibility(cam)
                if(it.isVisible) modelBatch.render(it.cache, environment)
            }
            arrows.forEach { (_, instance) ->
                modelBatch.render(instance, environment)
            }
        }
        modelBatch.end()
        // Render FPS counter
        spriteBatch.begin()
        font.draw(spriteBatch, "FPS: ${Gdx.graphics.framesPerSecond}", 10f, Gdx.graphics.height - 10f)
        font.draw(spriteBatch, "Time: $currentTime", 10f, Gdx.graphics.height - 30f)
        spriteBatch.end()

        // Render UI
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun dispose() {
        modelBatch.dispose()
        spriteBatch.dispose()
        chunks.values.forEach { c -> c.values.forEach {
            try {
                it.instance.model.dispose()
            } catch (e: IllegalArgumentException) { //buffer may already be disposed
                //TODO: log
            }
        }}
        popup?.dispose()
        menu?.dispose()
        font.dispose()
    }

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}


    private fun getChunkKey(xCoord: Float, zCoord: Float): String {
        //floor to nearest multiple of CHUNK_SIZE
        val x = Math.floorDiv(xCoord.toInt(), chunkSize) * chunkSize
        val z = Math.floorDiv(zCoord.toInt(), chunkSize) * chunkSize
        return "$x:$z"
    }

    fun upsertBuilding(data: Building) {
        CoroutineScope(Dispatchers.IO).launch {
            val model = data.toModel() ?: defaultBoxModel
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
            val convertedCoords = data.coords.toSceneCoords(this@MainScene.baselineCoord)
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
            chunks.getOrPut(
                getChunkKey(convertedCoords.x.toFloat(), convertedCoords.z.toFloat())
            )
            { ConcurrentHashMap() }[data.id] = GraphicalBuilding(data, model, inst)
        }
    }

    private suspend fun <T> runOnRenderThread(block: () -> T): T {
        return suspendCoroutine { continuation ->
            Gdx.app.postRunnable {
                continuation.resume(block())
            }
        }
    }

    private suspend fun Building.toModel(): Model? {
        if (this.ways.isEmpty()) return null
        val baseNodes = this.ways.first().nodes.map { node ->
            val x = node.coords - this.coords
            x.toSceneCoords(this@MainScene.baselineCoord, true)
        }.map { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }
            .distinct() //the last node is repeated for closed loops!
        if (baseNodes.size < 3) return null

        val height = this.tags.firstOrNull { it.key == "height" }?.value?.toFloatOrNull()
            ?: this.tags.firstOrNull { it.key == "building:levels" }?.value?.toFloatOrNull()
            ?: 2f
        val topNodes = baseNodes.map { it.cpy().add(0f, height, 0f) }

        val floats = baseNodes.flatMap { listOf(it.x, it.z) }.toFloatArray()
        val triangles: ShortArray
        val triangulator = EarClippingTriangulator()
        triangles = triangulator.computeTriangles(floats)


        return runOnRenderThread {
            val modelBuilder = ModelBuilder()
            modelBuilder.begin()
            var builder: MeshPartBuilder =
                modelBuilder.part("bottom", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            baseNodes.let {
                for (tri in 0..triangles.size - 2 step 3) {
                    builder.triangle(
                        it[triangles[tri].toInt()], it[triangles[tri + 1].toInt()], it[triangles[tri + 2].toInt()]
                    )
                }
            }

            builder =
                modelBuilder.part("top", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            topNodes.let {
                for (tri in 0..<triangles.size - 2 step 3) {
                    builder.triangle(
                        it[triangles[tri].toInt()], it[triangles[tri + 1].toInt()], it[triangles[tri + 2].toInt()]
                    )
                }
            }
            builder =
                modelBuilder.part("sides", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            val clockwise = baseNodes.isClockwise()
            for (i in 0..<baseNodes.lastIndex) {
                if (clockwise) {
                    builder.triangle(baseNodes[i], baseNodes[i + 1], topNodes[i + 1])
                    builder.triangle(topNodes[i + 1], topNodes[i], baseNodes[i])
                } else {
                    builder.triangle(baseNodes[i + 1], baseNodes[i], topNodes[i + 1])
                    builder.triangle(topNodes[i], topNodes[i + 1], baseNodes[i])
                }
            }
            //connect last and first
            if (clockwise) {
                builder.triangle(topNodes.first(), baseNodes.first(), baseNodes.last())
                builder.triangle(topNodes.first(), topNodes.last(), baseNodes.last())
            } else {
                builder.triangle(baseNodes.first(), baseNodes.last(), topNodes.last())
                builder.triangle(baseNodes.first(), topNodes.last(), topNodes.first())
            }
            modelBuilder.end()
        }
    }

    private fun List<Vector3>.isClockwise(): Boolean {
        var sum = 0f
        for (i in indices) {
            val v1 = this[i]
            val v2 = this[(i + 1) % this.size]
            sum += (v2.x - v1.x) * (v2.z + v1.z)
        }
        return sum > 0
    }

    suspend fun moveAgents(received: List<Agent>, time: Instant) {
        currentTime = time
        received.forEach { agent ->
            val prev = agents[agent.id]?.location?.toSceneCoords(this@MainScene.baselineCoord)
            agents[agent.id] = agent
            val current = agent.location.toSceneCoords(this@MainScene.baselineCoord)
            if (prev != null) {
                runOnRenderThread {
                    (arrows.putIfAbsent(agent.id, ModelInstance(arrowModel)) ?: arrows[agent.id])!!.apply {
                        //transform to point from prev to current
                        transform.setToTranslation(Vector3(prev.x.toFloat(), prev.y.toFloat(), prev.z.toFloat()))
                        /*val angle = (current - prev).let {
                            Math.toDegrees(asin(it.z/it.x)).toFloat()
                        }
                        transform.rotate(Vector3(0f, 1f, 0f), angle)*/
                    }
                }
            }
        }
    }

    fun pickBuildingRay(screenCoordX: Int, screenCoordY: Int): Pair<Float, GraphicalBuilding>? {
        //cast ray from screen coordinates
        val inter = Vector3()
        val ray = cam.getPickRay(screenCoordX.toFloat(), screenCoordY.toFloat())
        Intersector.intersectRayPlane(ray, Plane(Vector3(0f, 1f, 0f), Vector3(0f,0f,0f)), inter)

        //check for intersection with buildings
        return chunks[getChunkKey(inter.x, inter.z)]?.values
            ?.mapNotNull { bldg ->
                if (Intersector.intersectRayBounds(ray, bldg.instance.getTransformedBoundingBox(), null)) {
                    ray.origin.dst(bldg.instance.getTransformedBoundingBox().getCenter(Vector3())) to bldg
                } else null
            }
            ?.minByOrNull { it.first }
    }

    fun showPopup(coordX: Int, coordY: Int, building: GraphicalBuilding) {
        if(popup?.isVisible == true) return
        val menuBounds = menu?.getBoundaries()
        //if click is within menu boundary, ignore as the building is covered
        menuBounds?.let {
            val invertedY = stage.height - coordY
            if(coordX >= it.bLeft.x && coordX <= it.tRight.x
                && invertedY >= it.bLeft.y && invertedY <= it.tRight.y
            ) return
        }
        popup = PopupWindow(stage, skin, building.apiData!!)
        popup!!.show()
    }

    private fun showMenu() {
        if(menu?.isVisible == true) return
        menu = GameMenu(stage, skin, stage.width, stage.height, this)
        menu!!.show()
    }

    private fun ModelInstance.getTransformedBoundingBox(): BoundingBox {
        val bounds = BoundingBox()
        calculateBoundingBox(bounds)
        bounds.mul(transform)
        return bounds
    }

    fun updateCaches() {
        runBlocking {
            chunks.keys.forEach { c ->
                val chunk = chunks[c] ?: return@forEach
                val cache = caches.getOrPut(c) { CacheObject(ModelCache(), Mutex(), c.split(":").let { Vector3(it[0].toFloat(), 0f, it[1].toFloat()) }, chunkSize ) }
                val modelCache = cache.cache
                val mutex = cache.lock
                if (mutex.isLocked) return@forEach
                mutex.lock()
                modelCache.begin()
                chunk.values.forEach { modelCache.add(it.instance) }
                runOnRenderThread { modelCache.end() }
                mutex.unlock()
                println(caches.size)
            }
        }
        menu?.loadingBar?.fadeOut()
    }

    fun requestBuildings() {
        menu?.loadingBar?.fadeIn()
        val source = cam.position?.toWorldCoords(baselineCoord)!!.copy(y = 0f)
        runBlocking { Main.socket.sendSocketMessage(BuildingRequest(source, 0.03f)) }
    }

    fun toggleSimulation() {
    }

}