package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import dev.voroscsoki.stratopolis.client.InstanceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess


class GameMenu(
    private val stage: Stage,
    private val skin: Skin,
    private var stageWidth: Float,
    private var stageHeight: Float,
    private val instance: InstanceData,
    private val scene: MainScene
) : Group() {
    private val menuBar: Container<Table>
    var loadingBar: Container<Table>

    private val menuBarWidth: Float
    private val buttonSize = 40f

    init {
        name = "PersistentGameUI"

        val loadDrawable = loadAsset("refresh.png")
        val settingsDrawable = loadAsset("settings.png")
        val exitDrawable = loadAsset("exit.png")


        val buttons = arrayOf(
            createImageButton(
                upColor = Color(0.2f, 0.6f, 0.2f, 1f),
                downColor = Color(0.1f, 0.4f, 0.1f, 1f),
                hoverColor = Color(0.3f, 0.7f, 0.3f, 1f),
                iconDrawable = loadAsset("play.png")
            ) {
                scene.showSimulationDialog()
            },
            createImageButton(
                upColor = Color(0.2f, 0.6f, 0.2f, 1f),
                downColor = Color(0.1f, 0.4f, 0.1f, 1f),
                hoverColor = Color(0.3f, 0.7f, 0.3f, 1f),
                iconDrawable = loadDrawable
            ) {
                CoroutineScope(Dispatchers.IO).launch { scene.updateCaches() }
            },
            createImageButton(
                upColor = Color(0.2f, 0.2f, 0.6f, 1f),
                downColor = Color(0.1f, 0.1f, 0.4f, 1f),
                hoverColor = Color(0.3f, 0.3f, 0.7f, 1f),
                iconDrawable = settingsDrawable
            ) {
                scene.showSettings()
            },
            createImageButton(
                upColor = Color(0.6f, 0.2f, 0.2f, 1f),
                downColor = Color(0.4f, 0.1f, 0.1f, 1f),
                hoverColor = Color(0.7f, 0.3f, 0.3f, 1f),
                iconDrawable = exitDrawable
            ) {
                Gdx.app.exit()
                exitProcess(0)
            }
        )
        menuBarWidth = (buttonSize * buttons.size)
        menuBar = packButtons(*buttons)
        loadingBar = loadingStatus()

        addActor(menuBar)
        addActor(loadingBar)

        stage.addActor(this)
    }

    private fun loadAsset(filename: String) =
        TextureRegionDrawable(TextureRegion(Texture(Gdx.files.internal("assets/$filename"))))

    private fun createImageButton(
        upColor: Color,
        downColor: Color,
        hoverColor: Color,
        iconDrawable: TextureRegionDrawable? = null,
        onClick: () -> Unit
    ): ImageButton {
        val style = ImageButton.ImageButtonStyle().apply {
            up = createColoredDrawable(upColor)
            down = createColoredDrawable(downColor)
            over = createColoredDrawable(hoverColor)

            if (iconDrawable != null) {
                imageUp = iconDrawable
                imageDown = iconDrawable.tint(Color(1f, 1f, 1f, 0.8f))  // Slightly dim the icon when pressed
                imageOver = iconDrawable
            }
        }

        return ImageButton(style).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onClick()
                }
            })
        }
    }

    private fun packButtons(vararg buttons: ImageButton): Container<Table> {

        val table = Table().apply {
            defaults().size(buttonSize)
            align(Align.top)
            buttons.forEach { button ->
                button.width = buttonSize
                button.height = buttonSize
                // Scale the drawable to fit the button size
                val drawable = button.style.imageUp as TextureRegionDrawable
                drawable.minWidth = buttonSize
                drawable.minHeight = buttonSize
                add(button)
            }
            pack()
        }

        return Container(table).apply {
            setSize(menuBarWidth, buttonSize)
            setPosition(
                stageWidth - width,
                stageHeight - height
            )
        }
    }

    private fun loadingStatus() : Container<Table> {
        val loaderSize = 30
        val loaderDrawable = TextureRegionDrawable(TextureRegion(createSegmentedLoader(loaderSize)))

        // Create an image that will rotate
        val spinningLoader = Image(loaderDrawable).apply {
            setSize(loaderSize.toFloat(), loaderSize.toFloat())
            setOrigin(Align.center)
            addAction(Actions.forever(Actions.rotateBy(360f, 1f)))
        }

        val table = Table().apply {
            defaults().pad(5f)
            align(Align.top)

            add(spinningLoader).size(loaderSize.toFloat()).align(Align.center)

            val label = Label("Loading...", this@GameMenu.skin)
            add(label).padLeft(2f)

            pack()
        }

        return Container(table).apply {
            background = createColoredDrawable(Color(0.2f, 0.2f, 0.2f, 1f))
            setSize(menuBarWidth, buttonSize)
            setPosition(
                stageWidth - width,
                stageHeight - menuBar.height - height
            )
            isVisible = false
        }
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    fun dispose() {
        clear()
    }

    fun screenResized(width: Float, height: Float) {
        stageWidth = width
        stageHeight = height
        menuBar.setPosition(
            stageWidth - menuBar.width,
            stageHeight - menuBar.height
        )
        loadingBar.setPosition(
            stageWidth - loadingBar.width,
            stageHeight - menuBar.height - loadingBar.height
        )
    }

    private fun createSegmentedLoader(size: Int): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        val segments = 8
        val centerX = size / 2
        val centerY = size / 2
        val radius = (size / 2) - 2

        for (i in 0 until segments) {
            val alpha = 1f - (i.toFloat() / segments)
            pixmap.setColor(1f, 1f, 1f, alpha)

            val angle = (i.toFloat() / segments) * 2 * Math.PI
            val x1 = centerX + (radius * cos(angle)).toInt()
            val y1 = centerY + (radius * sin(angle)).toInt()
            val x2 = centerX + ((radius / 2) * cos(angle)).toInt()
            val y2 = centerY + ((radius / 2) * sin(angle)).toInt()

            pixmap.drawLine(x1, y1, x2, y2)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun getBoundaries() : UserInterfaceBounds {
        val bLeft = if(loadingBar.isVisible) UICoord(loadingBar.right - loadingBar.width, loadingBar.top - loadingBar.height)
            else UICoord(menuBar.right - menuBar.width, menuBar.top - menuBar.height)
        val tRight = UICoord(menuBar.right, menuBar.top)
        return UserInterfaceBounds(bLeft, tRight)
    }
}

data class UICoord(val x: Float, val y: Float)
data class UserInterfaceBounds(val bLeft: UICoord, val tRight: UICoord)

fun Container<Table>.fadeIn() {
    addAction(Actions.alpha(0f))
    isVisible = true
    addAction(
        Actions.fadeIn(0.5f)
    )
}
fun Container<Table>.fadeOut() {
    addAction(Actions.sequence(
        Actions.fadeOut(0.5f)
    ))
    isVisible = false
}