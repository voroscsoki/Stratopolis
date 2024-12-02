
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import dev.voroscsoki.stratopolis.common.util.Vec3
import kotlin.math.ceil

class HeatmapOverlay(
    private val gridSize: Int,      // Number of cells per row/column
    val cellSize: Float,    // Size of each grid cell
    private val chunkSize: Int = 32 // Size of each chunk (e.g., 32x32 cells)
) {
    private val grid = Array(gridSize) { IntArray(gridSize) } // Frequency grid
    private val meshes = mutableListOf<Mesh>()               // List of sub-meshes
    private var needsUpdate = true                           // Whether meshes need to be updated

    private val vertexShader = """
        #version 330 core
        in vec3 a_position;
        in vec4 a_color;

        uniform mat4 u_projTrans;

        out vec4 v_color;

        void main() {
            v_color = a_color;
            gl_Position = u_projTrans * vec4(a_position, 1.0);
        }

    """.trimIndent()
    private val fragmentShader = """
        #version 330 core
        in vec4 v_color;

        out vec4 fragColor;

        void main() {
            fragColor = v_color;
        }

    """.trimIndent()

    private val shader: ShaderProgram = ShaderProgram(
        vertexShader,
        fragmentShader
    )

    fun updateFrequency(coords: Vec3, value: Int) {
        val gridX = ((coords.x / cellSize).toInt() + gridSize / 2).coerceIn(0, gridSize - 1)
        val gridZ = ((coords.z / cellSize).toInt() + gridSize / 2).coerceIn(0, gridSize - 1)
        grid[gridX][gridZ] = value
        needsUpdate = true
    }

    fun compile() {
        if (!needsUpdate) return
        needsUpdate = false

        meshes.forEach { it.dispose() }
        meshes.clear()

        val maxFrequency = grid.maxOf { it.maxOrNull() ?: 0 }

        val chunksPerRow = ceil(gridSize / chunkSize.toFloat()).toInt()
        for (chunkX in 0 until chunksPerRow) {
            for (chunkZ in 0 until chunksPerRow) {
                val vertices = mutableListOf<Float>()
                val indices = mutableListOf<Short>()
                var vertexCount = 0

                for (x in 0 until chunkSize) {
                    for (z in 0 until chunkSize) {
                        val globalX = chunkX * chunkSize + x
                        val globalZ = chunkZ * chunkSize + z

                        if (globalX >= gridSize || globalZ >= gridSize) continue

                        val freq = grid[globalX][globalZ]
                        if(freq == 0) continue
                        val strength = (freq.toFloat() / maxFrequency).coerceIn(0f, 1f)

                        val worldX = (globalX - gridSize / 2) * cellSize
                        val worldZ = (globalZ - gridSize / 2) * cellSize
                        val red = (strength / 0.5f).coerceAtMost(1f)
                        val green = ((1 - strength) / 0.5f).coerceAtMost(1f)
                        val color = listOf(red, green, 0f, 0.5f)

                        //build quad
                        vertices.addAll(
                            listOf(
                                worldX, 0f, worldZ, color[0], color[1], color[2], color[3],
                                worldX + cellSize, 0f, worldZ, color[0], color[1], color[2], color[3],
                                worldX + cellSize, 0f, worldZ + cellSize, color[0], color[1], color[2], color[3],
                                worldX, 0f, worldZ + cellSize, color[0], color[1], color[2], color[3]
                            )
                        )

                        indices.addAll(
                            listOf(
                                (vertexCount + 0).toShort(),
                                (vertexCount + 1).toShort(),
                                (vertexCount + 2).toShort(),
                                (vertexCount + 2).toShort(),
                                (vertexCount + 3).toShort(),
                                (vertexCount + 0).toShort()
                            )
                        )

                        vertexCount += 4
                    }
                }

                if (vertices.isEmpty()) continue

                val mesh = Mesh(
                    true,
                    vertices.size / 7,
                    indices.size,
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.ColorUnpacked()
                    )
                )
                mesh.setVertices(vertices.toFloatArray())
                mesh.setIndices(indices.toShortArray())
                meshes.add(mesh)
            }
        }
    }

    fun render(cameraMatrix: Matrix4) {
        if (meshes.isEmpty()) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shader.bind()
        shader.setUniformMatrix("u_projTrans", cameraMatrix)

        meshes.forEach { mesh ->
            mesh.render(shader, GL20.GL_TRIANGLES)
        }

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun dispose() {
        meshes.forEach { it.dispose() }
        shader.dispose()
    }
}
