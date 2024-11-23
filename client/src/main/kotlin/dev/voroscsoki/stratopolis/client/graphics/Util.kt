package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
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

fun createColoredDrawable(color: Color): TextureRegionDrawable {
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
        setColor(color)
        fill()
    }
    val texture = Texture(pixmap)
    pixmap.dispose()

    return TextureRegionDrawable(TextureRegion(texture))
}