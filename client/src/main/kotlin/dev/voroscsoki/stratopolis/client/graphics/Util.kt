package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Window
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


fun createDefaultGdxSkin(): Skin {
    val skin = Skin()

    // Create a default font (LibGDX has a built-in bitmap font)
    val font = BitmapFont() // Automatically uses a default built-in font
    skin.add("default-font", font)

    // Create a white texture for backgrounds and borders
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.WHITE)
    pixmap.fill()
    val whiteTexture = Texture(pixmap)
    skin.add("default-texture", TextureRegion(whiteTexture))

    // Create a drawable from the white texture
    val whiteDrawable = TextureRegionDrawable(TextureRegion(whiteTexture))
    skin.add("default", whiteDrawable)

    // Label style
    val labelStyle = Label.LabelStyle().apply {
        this.font = font
        this.fontColor = Color.WHITE
    }
    skin.add("default", labelStyle)

    // Button style
    val buttonStyle = TextButton.TextButtonStyle().apply {
        this.up = whiteDrawable.tint(Color.DARK_GRAY)
        this.down = whiteDrawable.tint(Color.GRAY)
        this.checked = whiteDrawable.tint(Color.DARK_GRAY)
        this.font = font
    }
    skin.add("default", buttonStyle)

    // Window style
    val windowStyle = Window.WindowStyle().apply {
        this.titleFont = font
        this.background = whiteDrawable.tint(Color.DARK_GRAY)
    }
    skin.add("default", windowStyle)

    pixmap.dispose()
    return skin
}