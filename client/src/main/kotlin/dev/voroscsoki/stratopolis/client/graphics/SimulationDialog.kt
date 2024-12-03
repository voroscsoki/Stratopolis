package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.common.SimulationData
import dev.voroscsoki.stratopolis.common.networking.SimulationRequest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*


class SimulationDialog(stage: Stage, skin: CustomSkin) : Window("Simulation start", skin) {
    private fun TextField.getTimeFilter(isHour: Boolean = false): TextField.TextFieldFilter {
        return TextField.TextFieldFilter { _, c ->
            if(!c.isDigit()) return@TextFieldFilter false
            val textAfter =
                if(this.selection == null) this.text + c
                else {
                    val selectionLength = this.selection!!.length
                    this.text.replace(this.selection, "") + c
                }
            return@TextFieldFilter textAfter.length < 3 && textAfter.toIntOrNull()?.let { it < (if (isHour) 24 else 60) } ?: false
        }
    }

    init {
        setSize(stage.viewport.worldWidth * 0.3f, stage.viewport.worldHeight * 0.3f)

        val mainTable = Table(skin)
        mainTable.setFillParent(true)
        add(mainTable).grow()

        val settingsTable = Table(skin).top().left()

        val startTimeLabel = Label("Start time: ", skin)
        val startHour = TextField("08", skin)
        startHour.textFieldFilter = startHour.getTimeFilter(true)
        val startMinute = TextField("00", skin)
        startMinute.textFieldFilter = startMinute.getTimeFilter(false)
        settingsTable.add(startTimeLabel).left().top().fillY().row()
        settingsTable.add(startHour).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f)
        settingsTable.add(startMinute).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f).row()

        val endTimeLabel = Label("End time: ", skin)
        val endHour = TextField("09", skin)
        endHour.textFieldFilter = endHour.getTimeFilter(true)
        val endMinute = TextField("00", skin)
        endMinute.textFieldFilter = endMinute.getTimeFilter(false)
        settingsTable.add(endTimeLabel).left().top().fillY().row()
        settingsTable.add(endHour).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f)
        settingsTable.add(endMinute).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f).row()

        val agentLabel = Label("Agent count: ", skin)
        val agentCount = TextField("20000", skin)
        agentCount.textFieldFilter = TextField.TextFieldFilter{ _, char ->
            char.isDigit()
        }
        settingsTable.add(agentLabel).left().top().fillY().row()
        settingsTable.add(agentCount).width(200f).expandX().fillX().left().padBottom(10f).padTop(5f).row()

        val sendButton = createTextButton("Send request", skin) { button ->
            val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startTime = LocalDateTime(date, LocalTime.parse("${startHour.text}:${startMinute.text}")).toInstant(UtcOffset.ZERO)
            val endTime = LocalDateTime(date, LocalTime.parse("${endHour.text}:${endMinute.text}")).toInstant(UtcOffset.ZERO)
            val simulationData = SimulationData(
                startTime, endTime, agentCount.text.toInt(), Main.instanceData.baselineCoord!!, Main.appScene.heatmap.cellSize / 100000.0)
            Main.instanceData.reset(startTime)
            Main.instanceData.graphicsLoading = true
            runBlocking { Main.socket.sendSocketMessage(SimulationRequest(simulationData)) }
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