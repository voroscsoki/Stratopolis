package dev.voroscsoki.stratopolis.client

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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.common.api.Vec3
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Basic3D : ApplicationListener {
    private lateinit var cam: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var model: Model
    private val chunks = ConcurrentHashMap<String, ConcurrentHashMap<Long, ModelInstance>>()
    private val visibleChunks = mutableSetOf("0:0:0")
    private val CHUNK_SIZE = 50f
    private lateinit var environment: Environment
    private lateinit var camController: CameraInputController
    private val rand = Random(0)
    private val baselineCoord = Vec3(47.4981399, 0.0, 19.0409544)
    private var renderCounter = 0

    // FPS counter variables
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont

    fun Vec3.coordConvert(): Vec3 {
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

        modelBatch = ModelBatch()
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 600f
            update()
        }

        val modelBuilder = ModelBuilder()
        model = modelBuilder.createBox(
            0.8f, 0.8f, 0.8f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        chunks.getOrPut(getChunkKey(Vec3(0.0, 0.0,0.0))) { ConcurrentHashMap() }[0] = ModelInstance(model)

        val multiplexer = InputMultiplexer().apply {
            camController = CameraInputController(cam)
            addProcessor(camController)
            addProcessor(MyInput())
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

        if(renderCounter++ == 20) {
            renderCounter = 0
            updateVisibleChunks()
        }

        modelBatch.begin(cam)
        chunks.filter { it.key in visibleChunks }.forEach {
            it.value.values.forEach { modelBatch.render(it, environment) }
        }
        modelBatch.end()

        // Render FPS counter
        spriteBatch.begin()
        font.draw(spriteBatch, "FPS: ${Gdx.graphics.framesPerSecond}", 10f, Gdx.graphics.height - 10f)
        spriteBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        spriteBatch.dispose()
        font.dispose()
    }

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}


    private fun getChunkKey(vec3: Vec3): String {
        val x = (vec3.x / CHUNK_SIZE).toInt()
        val y = (vec3.y / CHUNK_SIZE).toInt()
        val z = (vec3.z / CHUNK_SIZE).toInt()
        return "$x:$y:$z"
    }

    private fun updateVisibleChunks() {
        val res = nearbyChunks(cam.position, 1)
        visibleChunks.clear()
        visibleChunks.addAll(res)
    }

    private fun nearbyChunks(position: Vector3, radius: Int): List<String> {
        val baseX = (position.x / CHUNK_SIZE).toInt()
        val baseY = (position.y / CHUNK_SIZE).toInt()
        val baseZ = (position.z / CHUNK_SIZE).toInt()
        return buildList {
            for(x in -radius..radius) {
                for (y in -radius..radius) {
                    for (z in -radius..radius) {
                        add("${baseX+x}:${baseY+y}:${baseZ+z}")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun upsertNode(data: SerializableNode) {
        val inst = ModelInstance(model)
        val convertedCoords = data.coords.coordConvert()
        val validVec = Vector3(convertedCoords.x.toFloat(), convertedCoords.y.toFloat(), convertedCoords.z.toFloat())
        inst.transform.setTranslation(validVec)
        inst.materials[0].set(ColorAttribute.createDiffuse(Color.valueOf(rand.nextLong().toHexString())))
        chunks.getOrPut(getChunkKey(convertedCoords)) { ConcurrentHashMap() }[data.id] = inst
    }

    fun upsertBuilding(data: Building) {
        val inst = ModelInstance(model)
        inst.transform.scale(2.0f, 2.0f, 2.0f)
        val convertedCoords = data.coords.coordConvert()
        val validVec = Vector3(convertedCoords.x.toFloat(), convertedCoords.y.toFloat(), convertedCoords.z.toFloat())
        inst.transform.setTranslation(validVec)
        inst.materials[0].set(ColorAttribute.createDiffuse(
            when (data.tags.find { it.key == "building" }?.value) {
                "commercial" -> Color.CYAN
                "house" -> Color.GREEN
                "industrial" -> Color.YELLOW
                else -> Color.RED
            }
        ))
        chunks.getOrPut(getChunkKey(convertedCoords)) { ConcurrentHashMap() }[data.id] = inst
    }
}