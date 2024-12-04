package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.networking.HttpAccessor
import dev.voroscsoki.stratopolis.client.networking.SocketClient
import games.spooky.gdx.nativefilechooser.NativeFileChooser
import games.spooky.gdx.nativefilechooser.NativeFileChooserCallback
import games.spooky.gdx.nativefilechooser.NativeFileChooserConfiguration
import games.spooky.gdx.nativefilechooser.desktop.DesktopFileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SettingsPage(stage: Stage, skin: CustomSkin) : Window("Settings", skin) {
    var fileChooser: NativeFileChooser = DesktopFileChooser()
    init {
        setSize(stage.viewport.worldWidth * 0.6f, stage.viewport.worldHeight * 0.4f)

        val mainTable = Table(skin)
        mainTable.setFillParent(true)
        add(mainTable).grow()

        val settingsTable = Table(skin).top().left()

        val label = Label("Server address: ", skin)
        settingsTable.add(label).left().top().fillY().row()

        val addressField = TextField(Main.socket.basePath, skin)
        settingsTable.add(addressField).width(400f).expandX().fillX().left().padBottom(20f).padTop(5f)

        val addressCheck = createTextButton("Check", skin) { button ->
            runBlocking {
                if(Main.socket.isWebSocketAvailable(addressField.text + "/control")) {
                    button.setText("Check: OK")
                } else {
                    button.setText("Check: Failed")
                }
            }
        }
        val loadServer = createTextButton("Load server", skin) { button ->
            CoroutineScope(Dispatchers.IO).launch {
                val soc = SocketClient(Main.socket.incomingHandler, addressField.text)
                if(soc.isWebSocketAvailable(soc.targetAddress)) soc.initializeWebSocket()
                Main.socket = soc
                Main.instanceData.clearGraphics()
                Main.instanceData.setupGame()
            }
        }
        settingsTable.add(addressCheck).padLeft(10f)
        settingsTable.add(loadServer).padLeft(10f)
        settingsTable.row()

        val fileOpenButton = createTextButton("Open file", skin) { button ->
            val conf = NativeFileChooserConfiguration()
            conf.directory = Gdx.files.absolute(System.getProperty("user.home"))
            conf.title = "Choose a PBF binary"

            fileChooser.chooseFile(conf, object : NativeFileChooserCallback {
                override fun onFileChosen(file: FileHandle?) {
                    runBlocking {
                        file?.let {
                            Main.instanceData.graphicsLoading = true
                            HttpAccessor.sendPbfFile(it.file())
                        }
                    }
                }

                override fun onCancellation() {}

                override fun onError(exception: Exception) {}
            })
        }
        settingsTable.add(fileOpenButton).padTop(20f).padBottom(20f).left()

        mainTable.add(settingsTable).expand().fill().pad(50f)
        mainTable.row()

        val closeButton = TextButton("Close", skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    CoroutineScope(Dispatchers.IO).launch {
                        hide()
                    }
                }
            })
        }

        mainTable.add(closeButton).bottom().pad(20f)

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