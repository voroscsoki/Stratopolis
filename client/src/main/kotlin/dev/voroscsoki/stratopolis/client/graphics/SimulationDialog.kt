package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener


class SimulationDialog(stage: Stage, skin: CustomSkin) : Window("Simulation start", skin) {
    init {
        setSize(stage.viewport.worldWidth * 0.4f, stage.viewport.worldHeight * 0.4f)

        val mainTable = Table(skin)
        mainTable.setFillParent(true)
        add(mainTable).grow()

        val settingsTable = Table(skin).top().left()

        val startTimeLabel = Label("Start time: ", skin)
        val startTimeDial = TextField("", skin)
        settingsTable.add(startTimeLabel).left().top().fillY().row()
        settingsTable.add(startTimeDial).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f).row()

        val endTimeLabel = Label("End time: ", skin)
        val endTimeDial = TextField("", skin)
        settingsTable.add(endTimeLabel).left().top().fillY().row()
        settingsTable.add(endTimeDial).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f).row()

        val sendButton = createTextButton("Send request", skin) { button ->
            val startTime = startTimeDial.text
            val endTime = endTimeDial.text
            println("Start time: $startTime, End time: $endTime")
            hide()
        }
        settingsTable.add(sendButton).padTop(20f).padBottom(20f).left()

        add(settingsTable).grow()

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

    private fun createTextButton(
        text: String, skin: CustomSkin,
        onClick: (TextButton) -> Unit
    ): TextButton {

        return TextButton(text, skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onClick(this@apply)
                }
            })
        }
    }
}