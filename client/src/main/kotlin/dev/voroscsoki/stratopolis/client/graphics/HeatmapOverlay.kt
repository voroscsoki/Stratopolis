
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import org.lwjgl.opengl.GL40
import java.util.concurrent.ConcurrentHashMap

class HeatmapOverlay {
    private val chunks = ConcurrentHashMap<String, DynamicNumberedChunk>()

    private val modelBuilder = ModelBuilder()
    private lateinit var modelBatch: ModelBatch
    private lateinit var chunkModel: Model
    private var chunkSize = 100f

    private val tempColor = Color(Color.RED)
    private val tempVector = Vector3()

    // Caching opacity calculations
    private val opacityCache = mutableMapOf<Int, Float>()

    data class DynamicNumberedChunk(
        val id: String,
        var number: Int,
        val modelInstance: ModelInstance,
        val position: Vector3
    ) {
        var needsMaterialUpdate = true
    }

    fun init(modelBatch: ModelBatch, chunkSize: Float = 100f) {
        this.modelBatch = modelBatch
        this.chunkSize = chunkSize
        val initialMaterial = createChunkMaterial(0)

        this.chunkModel = modelBuilder.createBox(
            chunkSize, 1f, chunkSize,
            initialMaterial,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    }

    fun addChunk(x: Float, z: Float, initialNumber: Int) {
        val chunkId = "$x:$z"

        chunks[chunkId]?.let { existingChunk ->
            existingChunk.number = initialNumber
            existingChunk.needsMaterialUpdate = true
            return
        }

        val modelInstance = ModelInstance(chunkModel)
        modelInstance.transform.setToTranslation(x, 100f, z)

        val res = DynamicNumberedChunk(chunkId, initialNumber, modelInstance, Vector3(x, 0f, z))
        updateChunkModelMaterial(res)
        chunks[chunkId] = res
    }

    private fun createChunkMaterial(number: Int): Material {
        val opacity = calculateOpacity(number)
        return Material(
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity),
            ColorAttribute(ColorAttribute.Diffuse, Color.RED)
        )
    }

    private fun calculateOpacity(number: Int): Float {
        // Use cached opacity to reduce redundant calculations
        return opacityCache.getOrPut(number) {
            (0.1f + (number.coerceIn(0, 10) / 10f)).coerceIn(0.1f, 0.8f)
        }
    }

    fun updateChunkNumber(x: Float, z: Float, newNumber: Int) {
        if(!chunks.contains("$x:$z")) {
            addChunk(x, z, newNumber)
            return
        }
        val chunkId = "$x:$z"
        chunks[chunkId]?.let { chunk ->
            if (chunk.number != newNumber) {
                chunk.number = newNumber
                chunk.needsMaterialUpdate = true
            }
        }
    }

    fun renderChunks(camera: PerspectiveCamera) {
        // Early exit if no chunks
        if (chunks.isEmpty()) return

        val visibleChunks = chunks.values.filter { isChunkVisible(it, camera) }
        println(visibleChunks)
        visibleChunks
                .filter { it.needsMaterialUpdate }
                .map { updateChunkModelMaterial(it) }

        Gdx.gl.glEnable(GL40.GL_BLEND)
        Gdx.gl.glBlendFunc(GL40.GL_SRC_ALPHA, GL40.GL_ONE_MINUS_SRC_ALPHA)

        modelBatch.begin(camera)
        modelBatch.render(visibleChunks.map { it.modelInstance })
        modelBatch.end()
    }

    private fun isChunkVisible(chunk: DynamicNumberedChunk, camera: PerspectiveCamera): Boolean {
        tempVector.set(chunk.position.x, 100f, chunk.position.z)
        return camera.frustum.sphereInFrustum(tempVector, chunkSize)
    }

    private fun updateChunkModelMaterial(chunk: DynamicNumberedChunk) {
        val newOpacity = calculateOpacity(chunk.number)

        chunk.modelInstance.materials.forEach { material ->
            material.get(BlendingAttribute.Type)?.let { blendingAttr ->
                (blendingAttr as BlendingAttribute).opacity = newOpacity
            }
        }
        chunk.needsMaterialUpdate = false
    }

    fun dispose() {
        // Dispose of the shared model
        chunkModel.dispose()

        chunks.clear()
        opacityCache.clear()
    }

    fun bulkUpdateChunks(updates: Map<Pair<Float, Float>, Int>) {
        updates.forEach { (coords, number) ->
            updateChunkNumber(coords.first, coords.second, number)
        }
    }
}