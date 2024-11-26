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
import com.badlogic.gdx.utils.ShortArray
import com.badlogic.gdx.utils.viewport.ScreenViewport
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.user_interface.UtilInput
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getWayAverage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


open class GraphicalObject(val instance: ModelInstance)
class GraphicalBuilding(val apiData: Building?, instance: ModelInstance) : GraphicalObject(instance)
class GraphicalArrow(var location: Vec3, instance: ModelInstance) : GraphicalObject(instance)
class GraphicalRoad(val apiData: Road?, instance: ModelInstance) : GraphicalObject(instance)

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
    lateinit var defaultBoxModel: Model
    lateinit var roadModel: Model
    private lateinit var arrowModel: Model
    private lateinit var environment: Environment

    private lateinit var stage: Stage
    private lateinit var skin: CustomSkin
    private var popup: PopupWindow? = null
    var menu: GameMenu? = null
    private var settingsPage: SettingsPage? = null

    val isCameraMoveEnabled : Boolean
        get() = settingsPage?.isVisible != true

    //constants
    private val chunkSize = 5000

    //updatables
    private val chunks = ConcurrentHashMap<String, ConcurrentHashMap<Long, GraphicalObject>>()
    private val caches = ConcurrentHashMap<String, CacheObject>()
    private val agents = ConcurrentHashMap<Long, Agent>()
    val arrows = ConcurrentHashMap<Long, GraphicalArrow>()

    //helper
    private var keyframeCounter = 0

    // text variables
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun create() {
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0f, 0f))
        }

        modelBatch = ModelBatch(DefaultShaderProvider()) { _, _ -> /*No sorting*/ }
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 1000f, 0f)
            lookAt(0f, 0f, 0f)
            up.set(1f, 0f, 0f)
            near = 1f
            far = 20000f
            update()
        }

        stage = Stage(ScreenViewport())
        skin = CustomSkin()

        modelBuilder = ModelBuilder()
        defaultBoxModel = modelBuilder.createBox(
            2f, 2f, 2f,
            Material(ColorAttribute.createDiffuse(Color.PURPLE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        roadModel = modelBuilder.createBox(
            1f, 0.1f, 1f,
            Material(ColorAttribute.createDiffuse(Color.GRAY)),
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
        chunks.getOrPut(getChunkKey(0.0, 0.0)) { ConcurrentHashMap() }[0] = GraphicalBuilding(null, inst)

        val multiplexer = InputMultiplexer().apply {
            addProcessor(CustomCameraController(this@MainScene))
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
        CoroutineScope(Dispatchers.IO).launch { Main.instanceData.setupGame() }
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
            arrows.forEach { (_, graphObj) ->
                graphObj.instance.transform.setTranslation(graphObj.location.let { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) })
                modelBatch.render(graphObj.instance, environment)
            }
        }
        modelBatch.end()
        // Render FPS counter
        spriteBatch.begin()
        font.draw(spriteBatch, "FPS: ${Gdx.graphics.framesPerSecond}", 10f, Gdx.graphics.height - 10f)
        font.draw(spriteBatch, "Time: ${Main.instanceData.currentTime}", 10f, Gdx.graphics.height - 30f)
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


    private fun getChunkKey(xCoord: Double, zCoord: Double): String {
        //floor to nearest multiple of CHUNK_SIZE
        val x = Math.floorDiv(xCoord.toInt(), chunkSize) * chunkSize
        val z = Math.floorDiv(zCoord.toInt(), chunkSize) * chunkSize
        return "$x:$z"
    }



    fun putBuilding(
        convertedCoords: Vec3,
        data: Building, inst: ModelInstance?
    ) {
        inst ?: return
        chunks.getOrPut(
            getChunkKey(convertedCoords.x, convertedCoords.z)
        )
        { ConcurrentHashMap() }[data.id] = GraphicalBuilding(data, inst)
    }

    fun putRoad(
        convertedCoords: Vec3,
        data: Road,
        inst: ModelInstance?
    ) {
        inst ?: return
        chunks.getOrPut(
            getChunkKey(convertedCoords.x, convertedCoords.z)
        )
        { ConcurrentHashMap() }[data.id] = GraphicalRoad(data, inst)
    }

    private suspend fun <T> runOnRenderThread(block: () -> T): T {
        return suspendCoroutine { continuation ->
            Gdx.app.postRunnable {
                continuation.resume(block())
            }
        }
    }

    suspend fun toModel(building: Building, baseline: Vec3): Model? {
        if (building.ways.isEmpty()) return null
        val baseNodes = building.ways.first().nodes.map { node ->
            val x = node.coords - building.coords
            x.toSceneCoords(baseline, true)
        }.map { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }
            .distinct() //the last node is repeated for closed loops!
        if (baseNodes.size < 3) return null

        val height = building.tags.firstOrNull { it.key == "height" }?.value?.toFloatOrNull()
            ?: building.tags.firstOrNull { it.key == "building:levels" }?.value?.toFloatOrNull()
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

    suspend fun toModel(data: Road, baseline: Vec3, width: Float = 3f): Model? {
        if (data.ways.isEmpty()) return null
        val baseNodes = data.ways.first().nodes.map { node ->
            val x = node.coords - data.ways.getWayAverage()
            x.toSceneCoords(baseline, true)
        }
        val prevailingDirection = (baseNodes[1] - baseNodes[0]).normalize()
        val perpendicular = Vec3(-prevailingDirection.z, 0.0, prevailingDirection.x) * (width / 2)
        val left = baseNodes.map { it + perpendicular }
        val right = baseNodes.map { it - perpendicular }
        val polygonalSeries = (left + right.reversed()).map { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

        val floats = polygonalSeries.flatMap { listOf(it.x, it.z) }.toFloatArray()
        val triangles: ShortArray
        val triangulator = EarClippingTriangulator()
        triangles = triangulator.computeTriangles(floats)

        return runOnRenderThread {
            val modelBuilder = ModelBuilder()
            modelBuilder.begin()
            val builder: MeshPartBuilder =
                modelBuilder.part("road", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.GRAY)))
            polygonalSeries.let {
                for (tri in 0..triangles.size - 2 step 3) {
                    builder.triangle(
                        it[triangles[tri].toInt()], it[triangles[tri + 1].toInt()], it[triangles[tri + 2].toInt()]
                    )
                }
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

    fun pickBuildingRay(screenCoordX: Int, screenCoordY: Int): Pair<Float, GraphicalBuilding>? {
        //cast ray from screen coordinates
        val inter = Vector3()
        val ray = cam.getPickRay(screenCoordX.toFloat(), screenCoordY.toFloat())
        Intersector.intersectRayPlane(ray, Plane(Vector3(0f, 1f, 0f), Vector3(0f,0f,0f)), inter)

        //check for intersection with buildings
        return chunks[getChunkKey(inter.x.toDouble(), inter.z.toDouble())]?.values?.mapNotNull { it as? GraphicalBuilding }
            ?.mapNotNull { bldg ->
                if (Intersector.intersectRayBounds(ray, bldg.instance.getTransformedBoundingBox(), null)) {
                    ray.origin.dst(bldg.instance.getTransformedBoundingBox().getCenter(Vector3())) to bldg
                } else null
            }
            ?.minByOrNull { it.first }
    }

    fun showPopup(coordX: Int, coordY: Int, building: GraphicalBuilding) {
        if(popup?.isVisible == true || settingsPage?.isVisible == true) return
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
        menu = GameMenu(stage, skin, stage.width, stage.height, Main.instanceData,this)
        menu!!.show()
    }

    fun showSettings() {
        if(settingsPage?.isVisible == true) return
        settingsPage = SettingsPage(stage, skin)
        settingsPage!!.show()
    }

    private fun ModelInstance.getTransformedBoundingBox(): BoundingBox {
        val bounds = BoundingBox()
        calculateBoundingBox(bounds)
        bounds.mul(transform)
        return bounds
    }

    fun clearGraphicalData() {
        chunks.clear()
        caches.clear()
        agents.clear()
        arrows.clear()
    }

    fun updateCaches() {
        runBlocking {
            chunks.keys.forEach { c ->
                val chunk = chunks[c] ?: return@forEach
                caches[c] = CacheObject(ModelCache(), Mutex(), c.split(":").let { Vector3(it[0].toFloat(), 0f, it[1].toFloat()) }, chunkSize )
                val modelCache = caches[c]!!.cache
                val mutex = caches[c]!!.lock
                if (mutex.isLocked) return@forEach
                mutex.withLock {
                    modelCache.begin()
                    chunk.values.partition { it is GraphicalBuilding }.let { parts ->
                        parts.second.forEach { modelCache.add(it.instance) }
                        parts.first.forEach { modelCache.add(it.instance) }
                    }
                    runOnRenderThread { modelCache.end() }
                }
            }
        }
    }

    suspend fun createArrow(): GraphicalArrow = GraphicalArrow(Vec3(0.0,0.0,0.0), runOnRenderThread { ModelInstance(defaultBoxModel).apply {
        this.transform.setToScaling(10f,10f,10f)
    } })
}

