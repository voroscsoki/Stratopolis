package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import dev.voroscsoki.stratopolis.common.util.Vec3


fun Vec3.toSceneCoords(baselineCoord: Vec3, scaleOnly: Boolean = false): Vec3 {
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

fun Vector3.toWorldCoords(baselineCoord: Vec3): Vec3 {
    val x = this.x / 100000 + baselineCoord.x
    val y = this.y / 100000 + baselineCoord.y
    val z = this.z / 100000 + baselineCoord.z
    return Vec3(x, y, z)
}

fun mergeMeshes(meshes: MutableList<Mesh>, transformations: AbstractList<Matrix4?>): Mesh? {
    if (meshes.isEmpty()) return null

    var vertexArrayTotalSize = 0
    var indexArrayTotalSize = 0

    val va = meshes[0].vertexAttributes
    val vaA = IntArray(va.size())
    for (i in 0..<va.size()) {
        vaA[i] = va[i].usage
    }

    for (i in meshes.indices) {
        val mesh = meshes[i]
        if (mesh.vertexAttributes.size() != va.size()) {
            meshes[i] = copyMesh(mesh, true, false, vaA)
        }

        vertexArrayTotalSize += mesh.numVertices * mesh.vertexSize / 4
        indexArrayTotalSize += mesh.numIndices
    }

    val vertices = FloatArray(vertexArrayTotalSize)
    val indices = ShortArray(indexArrayTotalSize)

    var indexOffset = 0
    var vertexOffset = 0
    var vertexSizeOffset = 0
    var vertexSize = 0

    for (i in 0..<meshes.size) {
        val mesh = meshes[i]

        val numIndices = mesh.numIndices
        val numVertices = mesh.numVertices
        vertexSize = mesh.vertexSize / 4
        val baseSize = numVertices * vertexSize
        val posAttr = mesh.getVertexAttribute(Usage.Position)
        val offset = posAttr.offset / 4
        val numComponents = posAttr.numComponents

        run {
            //uzupelnianie tablicy indeksow
            mesh.getIndices(indices, indexOffset)
            for (c in indexOffset..<(indexOffset + numIndices)) {
                indices[c] = (indices[c] + vertexOffset).toShort()
            }
            indexOffset += numIndices
        }

        mesh.getVertices(0, baseSize, vertices, vertexSizeOffset)
        Mesh.transform(transformations[i], vertices, vertexSize, offset, numComponents, vertexOffset, numVertices)
        vertexOffset += numVertices
        vertexSizeOffset += baseSize
    }

    val result = Mesh(true, vertexOffset, indices.size, meshes[0].vertexAttributes)
    result.setVertices(vertices)
    result.setIndices(indices)
    return result
}

fun copyMesh(meshToCopy: Mesh, isStatic: Boolean, removeDuplicates: Boolean, usage: IntArray?): Mesh {
    // TODO move this to a copy constructor?
    // TODO duplicate the buffers without double copying the data if possible.
    // TODO perhaps move this code to JNI if it turns out being too slow.
    val vertexSize = meshToCopy.vertexSize / 4
    var numVertices = meshToCopy.numVertices
    var vertices = FloatArray(numVertices * vertexSize)
    meshToCopy.getVertices(0, vertices.size, vertices)
    var checks: ShortArray? = null
    var attrs: Array<VertexAttribute?>? = null
    var newVertexSize = 0
    if (usage != null) {
        var size = 0
        var nullSize = 0
        for (i in usage.indices) if (meshToCopy.getVertexAttribute(usage[i]) != null) {
            size += meshToCopy.getVertexAttribute(usage[i]).numComponents
            nullSize++
        }
        if (size > 0) {
            attrs = arrayOfNulls(nullSize)
            checks = ShortArray(size)
            var idx = -1
            var ai = -1
            for (i in usage.indices) {
                val a = meshToCopy.getVertexAttribute(usage[i]) ?: continue
                for (j in 0..<a.numComponents) checks[++idx] = (a.offset / 4 + j).toShort()
                attrs[++ai] = VertexAttribute(a.usage, a.numComponents, a.alias)
                newVertexSize += a.numComponents
            }
        }
    }
    if (checks == null) {
        checks = ShortArray(vertexSize)
        for (i in 0..<vertexSize) checks[i.toInt()] = i.toShort()
        newVertexSize = vertexSize
    }

    val numIndices = meshToCopy.numIndices
    var indices: ShortArray? = null
    if (numIndices > 0) {
        indices = ShortArray(numIndices)
        meshToCopy.getIndices(indices)
        if (removeDuplicates || newVertexSize != vertexSize) {
            val tmp = FloatArray(vertices.size)
            var size = 0
            for (i in 0..<numIndices) {
                val idx1 = indices[i] * vertexSize
                var newIndex: Short = -1
                if (removeDuplicates) {
                    var j: Short = 0
                    while (j < size && newIndex < 0) {
                        val idx2 = j * newVertexSize
                        var found = true
                        var k = 0
                        while (k < checks.size && found) {
                            if (tmp[idx2 + k] != vertices[idx1 + checks[k]]) found = false
                            k++
                        }
                        if (found) newIndex = j
                        j++
                    }
                }
                if (newIndex > 0) indices[i] = newIndex
                else {
                    val idx = size * newVertexSize
                    for (j in checks.indices) tmp[idx + j] = vertices[idx1 + checks[j]]
                    indices[i] = size.toShort()
                    size++
                }
            }
            vertices = tmp
            numVertices = size
        }
    }
    val result = if (attrs == null) Mesh(isStatic, numVertices, indices?.size ?: 0, meshToCopy.vertexAttributes)
    else Mesh(isStatic, numVertices, indices?.size ?: 0, *attrs)
    result.setVertices(vertices, 0, numVertices * newVertexSize)
    result.setIndices(indices)
    return result
}


fun createDefaultGdxSkin(): Skin {
    val skin = Skin()

    val primaryColor = Color(0.2f, 0.6f, 0.9f, 1f)
    val accentColor = Color(0.95f, 0.5f, 0.2f, 1f)
    val backgroundColor = Color(0.15f, 0.15f, 0.18f, 1f)
    val surfaceColor = Color(0.2f, 0.2f, 0.24f, 1f)
    val textColor = Color(0.92f, 0.92f, 0.92f, 1f)

    val generator = FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))
    val normalText = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
        size = 14
        color = textColor
        borderWidth = 0f
    }
    val normalFont = generator.generateFont(normalText)
    skin.add("default-font", normalFont)

    val titleText = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
        size = 16
        color = textColor
        borderWidth = 0f
    }
    val titleFont = generator.generateFont(titleText)
    skin.add("title-font", titleFont)

    //UI texture
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.WHITE)
    pixmap.fill()
    val baseTexture = Texture(pixmap)
    val baseRegion = TextureRegion(baseTexture)
    val baseDrawable = TextureRegionDrawable(baseRegion)

    fun createPaddedDrawable(color: Color, leftRight: Float = 12f, topBottom: Float = 8f): Drawable {
        val drawable = TextureRegionDrawable(baseRegion).tint(color)
        drawable.leftWidth = leftRight
        drawable.rightWidth = leftRight
        drawable.topHeight = topBottom
        drawable.bottomHeight = topBottom
        return drawable
    }

    val labelStyle = Label.LabelStyle().apply {
        this.font = normalFont
        this.fontColor = textColor
    }
    skin.add("default", labelStyle)

    val buttonStyle = TextButton.TextButtonStyle().apply {
        this.up = createPaddedDrawable(primaryColor)
        this.down = createPaddedDrawable(primaryColor.cpy().mul(0.8f))
        this.over = createPaddedDrawable(primaryColor.cpy().mul(1.2f))
        this.font = normalFont
        this.fontColor = textColor
    }
    skin.add("default", buttonStyle)

    val accentButtonStyle = TextButton.TextButtonStyle(buttonStyle).apply {
        this.up = baseDrawable.tint(accentColor)
        this.down = baseDrawable.tint(accentColor.cpy().mul(0.8f))
        this.over = baseDrawable.tint(accentColor.cpy().mul(1.2f))
    }
    skin.add("accent", accentButtonStyle)

    val windowStyle = Window.WindowStyle().apply {
        this.titleFont = titleFont  // Using larger font for window titles
        this.background = baseDrawable.tint(surfaceColor)
        this.titleFontColor = textColor

        this.background.leftWidth = 16f
        this.background.rightWidth = 16f
        this.background.topHeight = 16f
        this.background.bottomHeight = 16f
    }
    skin.add("default", windowStyle)

    val textFieldStyle = TextField.TextFieldStyle().apply {
        this.font = normalFont
        this.fontColor = textColor
        this.background = baseDrawable.tint(backgroundColor)
        this.focusedBackground = baseDrawable.tint(backgroundColor.cpy().mul(1.2f))
        this.cursor = baseDrawable.tint(primaryColor)
        this.selection = baseDrawable.tint(primaryColor.cpy().mul(0.5f))

        this.background.leftWidth = 8f
        this.background.rightWidth = 8f
        this.background.topHeight = 8f
        this.background.bottomHeight = 8f
    }
    skin.add("default", textFieldStyle)

    pixmap.dispose()
    generator.dispose()
    return skin
}
