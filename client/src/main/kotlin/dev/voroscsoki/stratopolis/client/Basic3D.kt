package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.common.api.Vec3
import okhttp3.internal.toHexString
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


class Basic3D : ApplicationListener {
    private var cam: PerspectiveCamera? = null
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var instances = ConcurrentHashMap<Long, ModelInstance>()
    private var environment: Environment? = null
    private var camController: CameraInputController? = null
    private val rand = Random(0)
    private val baselineCoord = Vec3(47.4981399, 0.0,19.0409544)

    fun Vec3.coordConvert() : Vec3 {
        val x = (this.x - baselineCoord.x) * 100000
        val y = (this.y - baselineCoord.y) * 100000
        val z = (this.z - baselineCoord.z) * 100000
        return Vec3(x, y, z)
    }

    override fun create() {
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        modelBatch = ModelBatch()
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam!!.position[10f, 10f] = 10f
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.near = 1f
        cam!!.far = 600f
        cam!!.update()

        val modelBuilder = ModelBuilder()
        model = modelBuilder.createBox(
            0.8f, 0.8f, 0.8f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (Usage.Position or Usage.Normal).toLong()
        )
        instances[1] = ModelInstance(model)

        val multiplexer = InputMultiplexer()
        camController = CameraInputController(cam)
        multiplexer.addProcessor(camController)
        multiplexer.addProcessor(MyInput())
        Gdx.input.inputProcessor = multiplexer
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL40.GL_COLOR_BUFFER_BIT or GL40.GL_DEPTH_BUFFER_BIT)

        modelBatch!!.begin(cam)
        instances.values.forEach {
            if(cam!!.frustum.pointInFrustum(it.transform.getTranslation(Vector3()))) {
                modelBatch!!.render(it, environment)
            }
        }
        modelBatch!!.end()
    }

    override fun dispose() {
        modelBatch?.dispose()
        model?.dispose()
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    fun isInFrustum(spot: Vector3) : Boolean {
        return cam!!.frustum.pointInFrustum(spot)
    }

    fun upsertNode(data: SerializableNode) {
        val inst = ModelInstance(model)
        //move instance to place indicated by data.coords
        val convertedCoords = data.coords.coordConvert()
        val validVec = Vector3(convertedCoords.x.toFloat(), convertedCoords.y.toFloat(), convertedCoords.z.toFloat())
        inst.transform.setTranslation(validVec)
        inst.materials[0].set(ColorAttribute.createDiffuse(Color.valueOf(rand.nextLong().toHexString())))
        instances[data.id] = inst
    }

    fun upsertBuilding(data: Building) {
        val pts = data.points.map { it.coords.coordConvert() }

    }
}