package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.common.networking.BuildingRequest
import kotlinx.coroutines.runBlocking

class GameMenu(
    private val stage: Stage,
    private val skin: Skin,
    private val stageWidth: Float,
    private val stageHeight: Float,
    private val scene: MainScene
) : Group() {
    private val menuButton: TextButton
    private val dropdownPanel: Container<Table>
    private var isDropdownOpen = false

    init {
        name = "PersistentGameUI"
        createDefaultRectDrawable(skin)

        menuButton = TextButton("Menu", skin).apply {
            setSize(100f, 40f)
            setPosition(stageWidth - 100f, stageHeight - 40f)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    toggleDropdown()
                }
            })
        }

        dropdownPanel = createDropdownPanel()
        addActor(dropdownPanel)
        addActor(menuButton)

        stage.addActor(this)
    }

    private fun createDropdownPanel(): Container<Table> {
        val panelTable = Table(skin).apply {
            background = skin.getDrawable("default-rect")
        }

        val settingsButton = TextButton("Request", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    runBlocking {
                        val source = scene.cam.position?.toWorldCoords(scene.baselineCoord)!!.copy(y = 0f)
                        Main.socket.sendSocketMessage(BuildingRequest(source, 0.03f)) }
                }
            })
        }
        settingsButton.pack()

        val quitButton = TextButton("Quit", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    println("Quitting game")
                }
            })
        }
        quitButton.pack()

        panelTable.add(settingsButton).fillX().padBottom(10f).align(Align.center)
        panelTable.row()
        panelTable.add(quitButton).fillX().align(Align.center)

        return Container(panelTable).apply {
            setSize(200f, 120f)
            setPosition(
                stageWidth - 150f,
                stageHeight
            )
            isVisible = false
        }
    }

    private fun toggleDropdown() {
        isDropdownOpen = !isDropdownOpen

        if (isDropdownOpen) {
            dropdownPanel.isVisible = true
            dropdownPanel.addAction(
                Actions.parallel(
                    Actions.moveBy(0f, -130f, 0.5f),
                    Actions.fadeIn(0.5f)
                )
            )
        } else {
            dropdownPanel.addAction(
                Actions.sequence(
                    Actions.fadeOut(0.5f),
                    Actions.run { dropdownPanel.isVisible = false }
                ))
            dropdownPanel.addAction(Actions.moveBy(0f, 130f, 0.5f))
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

    private fun createDefaultRectDrawable(skin: Skin) {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color(0.2f, 0.2f, 0.2f, 0.8f))  // Dark, slightly transparent background
            fill()
        }
        val texture = Texture(pixmap)
        val ninePatch = NinePatch(texture, 1, 1, 1, 1)
        skin.add("default-rect", ninePatch)
        pixmap.dispose()
    }
}