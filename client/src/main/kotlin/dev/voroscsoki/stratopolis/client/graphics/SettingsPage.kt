package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

class SettingsPage(stage: Stage, skin: Skin) : Window("Settings", skin) {
    init {
        setSize(stage.viewport.worldWidth * 0.8f, stage.viewport.worldHeight * 0.8f)
        top().left()
        val serverAddress = TextField("ws://localhost:8085/control", skin).apply {
            width = 400f
            this@SettingsPage.add(this).pad(50f).row()
            pack()
        }

        val closeButton = TextButton("Close", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    hide()
                }
            })
            this@SettingsPage.add(this).padTop(20f).row()
            pack()
        }



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

    fun dispose() {
        remove()
    }
}