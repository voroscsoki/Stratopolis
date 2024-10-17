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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.client.util.VertexSorter
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.Vec3
import org.lwjgl.opengl.GL40
import kotlin.random.Random


class Basic3D : ApplicationListener {
    lateinit var cam: PerspectiveCamera
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    var instances = mutableListOf<ModelInstance>()
    lateinit var camController: CameraInputController
    lateinit var basicModel: Model
    private var position = Vector3()
    private var BASELINE_COORD = Vec3(47.4979, 0.0, 19.0402)
    val rand = Random(0)

    fun addBuilding(building: Building) {
        val instance =
            if (building.points.isEmpty()) ModelInstance(basicModel)
            else {
                val modelBuilder = ModelBuilder()
                val baseVertices = mutableListOf<Vector3>()
                val coords = building.points.map { it.coords }
                coords.map { it.coordScale() }.forEach { (x, y) ->
                    baseVertices.add(Vector3(x.toFloat(), 0f, y.toFloat()))  // Base vertices at height 0
                }
                val model = createBuildingModel(modelBuilder, baseVertices)

                ModelInstance(model)
            }

        building.coords.coordScale().let {
            instance.transform.setTranslation(it.x.toFloat(), 0f, it.z.toFloat())
        }
        instances.add(instance)
    }

    private fun createBuildingModel(
        modelBuilder: ModelBuilder,
        baseVertices: MutableList<Vector3>
    ): Model {
        modelBuilder.begin()
        val sortedVertices = VertexSorter.sort(baseVertices, Vector3(0f,1f,0f))
        val vertices = sortedVertices.map { VertexInfo().setPos(it).setNor(Vector3.Y) }.toTypedArray()
        modelBuilder.part("building", GL40.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.BLUE)))
            .apply {
                for (i in 0 until vertices.size - 2) {
                    triangle(vertices[0], vertices[i + 1], vertices[i + 2])
                }
            }
        //extrude the base to the height
        val height = 6f
        val topVertices = sortedVertices.map { it.cpy().add(0f, height, 0f) }
        val topVerticesInfo = topVertices.map { VertexInfo().setPos(it).setNor(Vector3.Y) }.toTypedArray()
        modelBuilder.part("building2", GL40.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.BLUE)))
            .apply {
                for (i in 0 until topVerticesInfo.size - 2) {
                    triangle(topVerticesInfo[0], topVerticesInfo[i + 1], topVerticesInfo[i + 2])
                }
            }
        //add sides
        for (i in sortedVertices.indices) {
            val prev = (i - 1 + sortedVertices.size) % sortedVertices.size
            modelBuilder.part("building3", GL40.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .apply {
                    this.rect(
                        sortedVertices[i], topVertices[i], topVertices[prev], sortedVertices[prev],
                        Vector3.Y
                    )
                }
        }
        return modelBuilder.end()
    }

    //treat baseline as 0,0, scale from there by 1000
    private fun Vec3.coordScale() = Vec3((this.x - BASELINE_COORD.x)*2000, 0.0, (this.z - BASELINE_COORD.z)*2000)


    override fun create() {
        val renderer = Gdx.gl.glGetString(GL20.GL_RENDERER)
        val version = Gdx.gl.glGetString(GL20.GL_VERSION)
        val vendor = Gdx.gl.glGetString(GL20.GL_VENDOR)
        Gdx.app.log("GPU Info", "Renderer: $renderer, Version: $version, Vendor: $vendor")
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        //height is Y
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position[0f, 100f] = 0f
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 2500f
        cam.rotate(Vector3(0f,1f,0f), -90f)
        cam.update()

        modelBatch = ModelBatch()
        basicModel = ModelBuilder().createBox(
            3f,3f,3f,
            Material(ColorAttribute.createDiffuse(Color.GREEN)),
            (Usage.Position or Usage.Normal).toLong()
        )
        //buildingInstances.add(ModelInstance(buildModel))

        camController = CameraInputController(cam)
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(camController)
        multiplexer.addProcessor(MyInput())
        Gdx.input.inputProcessor = multiplexer
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)  // Enable depth test
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)   // Enable face culling
        //Gdx.gl.glCullFace(GL20.GL_BACK)      // Cull back faces

    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        camController.update()
        modelBatch.begin(cam)
        for (instance in instances) {
            if (!isVisible(cam, instance)) continue
            modelBatch.render(instance, environment)

        }
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        basicModel.dispose()
    }

    private fun isVisible(cam: Camera, instance: ModelInstance): Boolean {
        instance.transform.getTranslation(position)
        return cam.frustum.sphereInFrustum(position, 1.5f)
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }
}
private fun Vector3.flatten() = floatArrayOf(this.x, this.y, this.z)
