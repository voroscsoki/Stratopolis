package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

class CustomSkin : Skin() {
    private val primaryColor = Color(0.2f, 0.6f, 0.9f, 1f)
    private val accentColor = Color(0.95f, 0.5f, 0.2f, 1f)
    private val backgroundColor = Color(0.15f, 0.15f, 0.18f, 1f)
    private val surfaceColor = Color(0.2f, 0.2f, 0.24f, 1f)
    private val textColor = Color(0.92f, 0.92f, 0.92f, 1f)

    private val generator = FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/Roboto-Regular.ttf"))
    val regularFontSize = 14
    private val titleFontSize = 16
    private val normalText = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
        size = regularFontSize
        color = textColor
        borderWidth = 0f
        minFilter = Texture.TextureFilter.Nearest
        magFilter = Texture.TextureFilter.Nearest
    }
    private val normalFont = generator.generateFont(normalText)

    private val titleText = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
        size = titleFontSize
        color = textColor
        borderWidth = 0f
    }
    private val titleFont = generator.generateFont(titleText)

    //UI texture
    private val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
        setColor(Color.WHITE)
        fill()
    }
    private val baseTexture = Texture(pixmap)
    private val baseRegion = TextureRegion(baseTexture)
    private val baseDrawable = TextureRegionDrawable(baseRegion)

    private val labelStyle = Label.LabelStyle().apply {
        this.font = normalFont
        this.fontColor = textColor
    }

    private val buttonStyle = TextButton.TextButtonStyle().apply {
        this.up = createPaddedDrawable(primaryColor)
        this.down = createPaddedDrawable(primaryColor.cpy().mul(0.8f))
        this.over = createPaddedDrawable(primaryColor.cpy().mul(1.2f))
        this.font = normalFont
        this.fontColor = textColor
    }


    private val accentButtonStyle = TextButton.TextButtonStyle(buttonStyle).apply {
        this.up = baseDrawable.tint(accentColor)
        this.down = baseDrawable.tint(accentColor.cpy().mul(0.8f))
        this.over = baseDrawable.tint(accentColor.cpy().mul(1.2f))
    }


    private val windowStyle = Window.WindowStyle().apply {
        titleFont = this@CustomSkin.titleFont  // Using larger font for window titles
        this.background = baseDrawable.tint(surfaceColor)
        this.titleFontColor = textColor

        this.background.leftWidth = 16f
        this.background.rightWidth = 16f
        this.background.topHeight = 16f
        this.background.bottomHeight = 16f
    }


    private val textFieldStyle = TextField.TextFieldStyle().apply {
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

        this.focusedBackground.leftWidth = 8f
        this.focusedBackground.rightWidth = 8f
        this.focusedBackground.topHeight = 8f
        this.focusedBackground.bottomHeight = 8f
    }

    init {
        this.add("default-font", normalFont)
        this.add("title-font", titleFont)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()

        this.add("default", labelStyle)
        this.add("default", buttonStyle)
        this.add("accent", accentButtonStyle)
        this.add("default", windowStyle)
        this.add("default", textFieldStyle)

        pixmap.dispose()
        generator.dispose()
    }




    private fun createPaddedDrawable(color: Color, leftRight: Float = 12f, topBottom: Float = 8f): Drawable {
        val drawable = TextureRegionDrawable(baseRegion).tint(color)
        drawable.leftWidth = leftRight
        drawable.rightWidth = leftRight
        drawable.topHeight = topBottom
        drawable.bottomHeight = topBottom
        return drawable
    }

}