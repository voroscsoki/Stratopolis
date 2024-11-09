package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.api.HttpAccessor
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
                launch { socket.initializeWebSocket() }
                asyncInit()
            }
        }
        private suspend fun asyncInit() {
            HttpAccessor.waitForConnection()
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Stratopolis")
            config.setWindowedMode(1600,900)
            config.useVsync(false)
            config.setForegroundFPS(120)
            Lwjgl3Application(appScene, config)
        }
    }
}
