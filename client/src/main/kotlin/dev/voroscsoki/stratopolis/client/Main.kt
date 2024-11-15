package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.networking.HttpAccessor
import dev.voroscsoki.stratopolis.client.graphics.Basic3D
import dev.voroscsoki.stratopolis.client.networking.SocketClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Main {
    companion object {
        val appScene = Basic3D()
        val instanceData = InstanceData()
        val socket = SocketClient(instanceData::handleIncomingMessage, "ws://localhost:8085/control")

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                HttpAccessor.waitForConnection()
                launch { socket.initializeWebSocket() }
                asyncInit()
            }
        }
        private suspend fun asyncInit() {

            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Stratopolis")
            config.setWindowedMode(1600,900)
            config.useVsync(true)
            config.setForegroundFPS(120)
            Lwjgl3Application(appScene, config)
        }
    }
}
