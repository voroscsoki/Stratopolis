package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder



class Basic3D : ApplicationListener {
    lateinit var cam: PerspectiveCamera
    lateinit var model: Model
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    lateinit var instance: ModelInstance
    lateinit var camController: CameraInputController

    override fun create() {
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position[10f, 10f] = 10f
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 300f
        cam.update()

        modelBatch = ModelBatch()
        val modelBuilder = ModelBuilder()
        model = modelBuilder.createBox(
            5f, 5f, 5f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (Usage.Position or Usage.Normal).toLong()
        )
        instance = ModelInstance(model)

        camController = CameraInputController(cam)
        Gdx.input.inputProcessor = camController
    }

    override fun render() {
        instance.transform.rotate(0f, 1f, 1f, 2f)
        
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        camController.update()

        modelBatch.begin(cam)
        modelBatch.render(instance, environment)
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }
}
