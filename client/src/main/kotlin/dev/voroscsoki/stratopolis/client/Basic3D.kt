package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.api.CoordPair
import org.lwjgl.opengl.GL40


class Basic3D : ApplicationListener {
    lateinit var cam: PerspectiveCamera
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    var buildingInstances = mutableListOf<ModelInstance>()
    lateinit var camController: CameraInputController
    lateinit var buildModel: Model
    private var position = Vector3()

    fun addBuilding(coords: CoordPair) {
        val instance = ModelInstance(buildModel)
        val newcoords = (coords.first - 47.5) * 10 to (coords.second - 19.1) * 10
        instance.transform.setTranslation(newcoords.first.toFloat(), 0f, newcoords.second.toFloat())
        buildingInstances.add(instance)
    }



    override fun create() {
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        //height is Y
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position[0f, 10f] = 0f
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 300f
        cam.rotate(Vector3(0f,1f,0f), -90f)
        cam.update()

        modelBatch = ModelBatch()
        buildModel = ModelBuilder().createBox(
            5f, 5f, 5f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (Usage.Position or Usage.Normal).toLong()
        )
        //buildingInstances.add(ModelInstance(buildModel))

        camController = CameraInputController(cam)
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(camController)
        multiplexer.addProcessor(MyInput())
        Gdx.input.inputProcessor = multiplexer
        GL40.glEnable(GL40.GL_CULL_FACE)
        GL40.glCullFace(GL40.GL_BACK)
    }

    override fun render() {
        //buildingInstances.first().transform.rotate(0f, 1f, 1f, 2f)
        
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        camController.update()

        modelBatch.begin(cam)
        for (instance in buildingInstances) {
            if (isVisible(cam, instance)) {
                modelBatch.render(instance, environment)
            }
        }
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        buildModel.dispose()
    }

    protected fun isVisible(cam: Camera, instance: ModelInstance): Boolean {
        instance.transform.getTranslation(position)
        return cam.frustum.pointInFrustum(position)
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }
}
