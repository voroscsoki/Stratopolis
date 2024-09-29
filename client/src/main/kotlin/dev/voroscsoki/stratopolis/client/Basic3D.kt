package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.CoordPair
import org.lwjgl.opengl.GL40
import kotlin.random.Random


class Basic3D : ApplicationListener {
    lateinit var cam: PerspectiveCamera
    lateinit var modelBatch: ModelBatch
    lateinit var environment: Environment
    var buildingInstances = mutableListOf<ModelInstance>()
    lateinit var camController: CameraInputController
    lateinit var basicModel: Model
    private var position = Vector3()
    private var BASELINE_COORD = CoordPair(47.4979, 19.0402)
    val rand = Random(0)

    fun addBuilding(building: Building) {
        val instance = //treat vertices as the base of the building, extrude upwards
            //treat vertices as the base of the building, extrude upwards
            if (building.points.isEmpty()) ModelInstance(basicModel)
            else {
                val modelBuilder = ModelBuilder()
                modelBuilder.begin()
                val meshBuilder = modelBuilder.part("part1", GL20.GL_TRIANGLES, (Usage.Position or Usage.Normal).toLong(), Material(ColorAttribute.createDiffuse(Color.CORAL)))

                val baseVertices = mutableListOf<Vector3>()
                val coords = building.points.map { it.coords }
                coords.map { it.coordScale() }.forEach { (x, y) ->
                    baseVertices.add(Vector3(x.toFloat(), y.toFloat(), 0f))  // Base vertices at height 0
                    baseVertices.add(Vector3(x.toFloat(), y.toFloat(), 5f)) // Top vertices at the specified height
                }

                // sides
                for (i in coords.indices) {
                    val nextIndex = (i + 1) % coords.size
                    val bottomLeft = i * 2
                    val bottomRight = nextIndex * 2
                    val topLeft = bottomLeft + 1
                    val topRight = bottomRight + 1

                    meshBuilder.rect(
                        baseVertices[bottomLeft], baseVertices[topLeft], baseVertices[topRight], baseVertices[bottomRight], Vector3(1f,0f,1f)
                    )
                }
                ModelInstance(modelBuilder.end())
            }

        building.coords.coordScale().let {
            instance.transform.setTranslation(it.first.toFloat(), 0f, it.second.toFloat())
        }
        buildingInstances.add(instance)
    }
    //treat baseline as 0,0, scale from there by 1000
    private fun CoordPair.coordScale() = CoordPair((this.first - BASELINE_COORD.first)*10000, (this.second - BASELINE_COORD.second)*10000)



    override fun create() {
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        //height is Y
        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position[0f, 10f] = 0f
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 5000f
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
                //instance.materials.first().set(ColorAttribute.createDiffuse(Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1f)))
                modelBatch.render(instance, environment)
            }
        }
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        basicModel.dispose()
    }

    protected fun isVisible(cam: Camera, instance: ModelInstance): Boolean {
        instance.transform.getTranslation(position)
        return cam.frustum.sphereInFrustum(position, 15f)
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }
}
