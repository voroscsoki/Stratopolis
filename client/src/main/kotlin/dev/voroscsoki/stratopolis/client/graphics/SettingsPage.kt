package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.networking.SocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsPage(stage: Stage, skin: CustomSkin) : Window("Settings", skin) {
    init {
        setSize(stage.viewport.worldWidth * 0.4f, stage.viewport.worldHeight * 0.4f)

        val mainTable = Table(skin)
        mainTable.setFillParent(true)
        add(mainTable).grow()

        val settingsTable = Table(skin).top().left()

        val label = Label("Server address: ", skin)
        settingsTable.add(label).right().top().fillY().padRight(10f)

        val addressField = TextField(Main.socket.targetAddress, skin)
        settingsTable.add(addressField).width(400f).expandX().fillX().left().padBottom(20f).padTop(5f)

        val addressCheck = createTextButton("Check", skin) { button ->
            runBlocking {
                if(Main.socket.isWebSocketAvailable(addressField.text)) {
                    button.setText("Check: OK")
                } else {
                    button.setText("Check: Failed")
                }
            }
        }
        settingsTable.add(addressCheck).padLeft(10f)
        settingsTable.row()

        mainTable.add(settingsTable).expand().fill().pad(50f)
        mainTable.row()

        val closeButton = TextButton("Close", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if(Main.socket.targetAddress != addressField.text) {
                            val soc = SocketClient(Main.socket.incomingHandler, addressField.text)
                                launch {
                                    if(soc.isWebSocketAvailable(soc.targetAddress)) soc.initializeWebSocket()
                                    Main.socket = soc
                                    Main.instanceData.setupGame()
                                }
                        }
                        hide()
                    }
                }
            })
        }

        mainTable.add(closeButton).bottom().left().pad(20f)

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