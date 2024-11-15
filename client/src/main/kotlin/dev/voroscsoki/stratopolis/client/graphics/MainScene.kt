package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ShortArray
import dev.voroscsoki.stratopolis.client.user_interface.UtilInput
import dev.voroscsoki.stratopolis.common.elements.Agent
import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class GraphicalBuilding(val apiData: Building?, val model: Model, val instance: ModelInstance)

class MainScene : ApplicationListener {
    //libGDX variables
    private lateinit var cam: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var modelBuilder: ModelBuilder
    private lateinit var defaultBoxModel: Model
    private lateinit var arrowModel: Model
    private lateinit var environment: Environment

    //constants
    private val chunkSize = 500
    private val baselineCoord = Vec3(47.472935, 0.0, 19.053410)

    //updatables
    private val chunks = ConcurrentHashMap<String, ConcurrentHashMap<Long, GraphicalBuilding>>()
    private val agents = ConcurrentHashMap<Long, Agent>()
    private val arrows = ConcurrentHashMap<Long, ModelInstance>()
    private var visibleChunks = setOf<String>()

    //helper
    private var renderCounter = 0
    private val position = Vector3()

    // text variables
    private var currentTime: Instant = Instant.fromEpochSeconds(0)
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont

    private fun Vec3.coordConvert(scaleOnly: Boolean = false): Vec3 {
        if(scaleOnly) {
            val x = this.x * 100000
            val y = this.y * 100000
            val z = this.z * 100000
            return Vec3(x, y, z)
        }
        val x = (this.x - baselineCoord.x) * 100000
        val y = (this.y - baselineCoord.y) * 100000
        val z = (this.z - baselineCoord.z) * 100000
        return Vec3(x, y, z)
    }

    override fun create() {
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

        modelBatch = ModelBatch(DefaultShaderProvider()) { _, _ -> /*No sorting*/ }
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 100f, 0f)
            lookAt(0f,0f,0f)
            up.set(1f,0f,0f)
            near = 1f
            far = 2000f
            update()
        }
        
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
        chunks.getOrPut(getChunkKey(0f,0f)) { ConcurrentHashMap() }[0] = GraphicalBuilding(null, defaultBoxModel, inst)

        val multiplexer = InputMultiplexer().apply {
            addProcessor(CustomCameraController(cam))
            addProcessor(UtilInput())
        }
        Gdx.input.inputProcessor = multiplexer
        Gdx.gl.glEnable(GL40.GL_CULL_FACE)
        Gdx.gl.glCullFace(GL40.GL_BACK)

        // Initialize FPS counter
        spriteBatch = SpriteBatch()
        font = BitmapFont()
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL40.GL_COLOR_BUFFER_BIT or GL40.GL_DEPTH_BUFFER_BIT)

        if (renderCounter++ == 20) {
            renderCounter = 0
            updateVisibleChunks()
        }
        cam.update()
        modelBatch.begin(cam)
        chunks.filter { visibleChunks.contains(it.key) }.forEach { chunk ->
            chunk.value.forEach { (_, element) ->
                if (isVisible(cam, element.instance)) modelBatch.render(element.instance, environment)
            }
        }
        arrows.forEach { (_, instance) ->
            modelBatch.render(instance, environment)
        }
        modelBatch.end()
        // Render FPS counter
        spriteBatch.begin()
        font.draw(spriteBatch, "FPS: ${Gdx.graphics.framesPerSecond}", 10f, Gdx.graphics.height - 10f)
        font.draw(spriteBatch, "Time: $currentTime", 10f, Gdx.graphics.height - 30f)
        spriteBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        spriteBatch.dispose()
        chunks.values.forEach { c -> c.values.forEach { it.model.dispose() } }
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

    private fun updateVisibleChunks() {
        val res = nearbyChunks(cam.position, 5)
        visibleChunks = res.toSet()
    }

    private fun nearbyChunks(position: Vector3, radius: Int): List<String> {
        val baseX = Math.floorDiv(position.x.toInt(), chunkSize) * chunkSize
        val baseZ = Math.floorDiv(position.z.toInt(), chunkSize) * chunkSize
        return buildList {
            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    add("${baseX + x * chunkSize}:${baseZ + z * chunkSize}")
                }
            }
        }
    }

    fun upsertBuilding(data: Building) {
        CoroutineScope(Dispatchers.IO).launch {
            val model = data.toModel() ?: defaultBoxModel
            val inst = ModelInstance(model)
            val convertedCoords = data.coords.coordConvert()
            val validVec =
                Vector3(convertedCoords.x.toFloat(), convertedCoords.y.toFloat(), convertedCoords.z.toFloat())
            inst.transform.setTranslation(validVec)
            inst.materials.forEach { material ->
                material.set(ColorAttribute.createDiffuse(
                    when (data.tags.find { it.key == "building" }?.value) {
                        "commercial" -> Color.BLUE
                        "house" -> Color.GREEN
                        "industrial" -> Color.YELLOW
                        else -> Color.CYAN
                    }
                ))
            }
            chunks.getOrPut(
                getChunkKey(convertedCoords.x.toFloat(), convertedCoords.z.toFloat()))
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

    private suspend fun Building.toModel() : Model? {
        if (this.ways.isEmpty()) return null
        var baseNodes = this.ways.first().nodes.map {
                node -> val x = node.coords - this.coords
            x.coordConvert(true)
        }.map { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }
        if(baseNodes.size < 3) return null

        val center = baseNodes.reduce { acc, vector3 -> acc.add(vector3) }.scl(1f / baseNodes.size)
        baseNodes.forEach { it.sub(center) }
        baseNodes = baseNodes.filter { it != Vector3(0f,0f,0f) }
        val height = this.tags.firstOrNull { it.key == "height" }?.value?.toFloatOrNull()
            ?: this.tags.firstOrNull { it.key == "building:levels"}?.value?.toFloatOrNull()
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

            builder = modelBuilder.part("top", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            topNodes.let {
                for (tri in 0..<triangles.size - 3 step 3) {
                    builder.triangle(
                        it[triangles[tri].toInt()], it[triangles[tri + 1].toInt()], it[triangles[tri + 2].toInt()]
                    )
                }
            }
            builder = modelBuilder.part("sides", GL40.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            val clockwise = baseNodes.isClockwise()
            for (i in 0..<baseNodes.lastIndex) {
                if(clockwise) {
                    builder.triangle(baseNodes[i], baseNodes[i+1], topNodes[i+1])
                    builder.triangle(topNodes[i+1], topNodes[i], baseNodes[i])
                } else {
                    builder.triangle(baseNodes[i+1], baseNodes[i], topNodes[i+1])
                    builder.triangle(topNodes[i], topNodes[i+1], baseNodes[i])
                }
            }
            modelBuilder.end()
        }
    }

    private fun isVisible(cam: Camera, instance: ModelInstance): Boolean {
        instance.transform.getTranslation(position)
        return cam.frustum.sphereInFrustum(position, 20f)
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
            val prev = agents[agent.id]?.location?.coordConvert()
            agents[agent.id] = agent
            val current = agent.location.coordConvert()
            if(prev != null) {
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
}