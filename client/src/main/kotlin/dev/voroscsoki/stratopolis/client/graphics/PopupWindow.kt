package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import dev.voroscsoki.stratopolis.common.elements.Building

class PopupWindow(private val stage: Stage, skin: Skin, building: Building) : Window("Popup", skin) {
    init {
        building.tags.toString().split(",").forEach {
            add(Label(it, skin)).row()
        }
        val closeButton = TextButton("Close", skin)
        closeButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                hide()
            }
        })
        add(closeButton).padTop(20f).row()
        pack()
        setPosition(
            stage.viewport.worldWidth / 2 - width / 2,
            stage.viewport.worldHeight / 2 - height / 2
        )
        isVisible = false
        stage.addActor(this)
    }
    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}