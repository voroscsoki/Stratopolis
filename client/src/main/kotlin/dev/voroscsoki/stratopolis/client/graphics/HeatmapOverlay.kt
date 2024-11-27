
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import java.util.concurrent.ConcurrentHashMap

class HeatmapOverlay {
    private val chunks = ConcurrentHashMap<String, DynamicNumberedChunk>()

    private val modelBuilder = ModelBuilder()
    private lateinit var modelBatch: ModelBatch
    private var chunkSize = 100f

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
    }

    fun addChunk(x: Float, z: Float, initialNumber: Int) {
        val chunkId = "$x:$z"
        val initialMaterial = createChunkMaterial(initialNumber)

        val chunkModel = modelBuilder.createBox(
            chunkSize, 1f, chunkSize,
            initialMaterial,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )

        val modelInstance = ModelInstance(chunkModel)
        modelInstance.transform.setToTranslation(x, 100f, z)

        chunks[chunkId] = DynamicNumberedChunk(chunkId, initialNumber, modelInstance, Vector3(x, 0f, z))
    }

    private fun createChunkMaterial(number: Int): Material {
        val opacity = calculateOpacity(number)
        return Material(
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, opacity),
            ColorAttribute(ColorAttribute.Diffuse, Color.RED)
        )
    }

    fun updateChunkNumber(x: Float, z: Float, newNumber: Int) {
        val chunkId = "$x:$z"
        chunks[chunkId]?.let { chunk ->
            chunk.number = newNumber
            chunk.needsMaterialUpdate = true
        }
    }

    private fun calculateOpacity(number: Int): Float {
        // scale between 0.1 and 1
        return (0.1f + (number.coerceIn(0, 10) / 10f)).coerceIn(0.1f, 1f)
    }

    fun renderChunks(camera: PerspectiveCamera) {
        modelBatch.begin(camera)

        chunks.values.forEach { chunk ->
            if (chunk.needsMaterialUpdate) {
                updateChunkModelMaterial(chunk)
            }

            modelBatch.render(chunk.modelInstance)
        }

        modelBatch.end()
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

    fun getChunk(x: Float, z: Float): DynamicNumberedChunk? {
        return chunks["$x:$z"]
    }

    fun getAllChunks(): List<DynamicNumberedChunk> {
        return chunks.values.toList()
    }

    fun removeChunk(x: Float, z: Float) {
        chunks.remove("$x:$z")
    }

    fun dispose() {
        chunks.values.forEach {
            it.modelInstance.model.dispose()
        }
        chunks.clear()
    }
}