package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.graphics.MainScene
import dev.voroscsoki.stratopolis.client.networking.SocketClient
import kotlinx.coroutines.runBlocking

class Main {
    companion object {
        val appScene = MainScene()
        val instanceData = InstanceData(appScene)
        var socket = SocketClient(instanceData::handleIncomingMessage)

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                if(socket.isWebSocketAvailable(socket.targetAddress)) socket.initializeWebSocket()
                val config = Lwjgl3ApplicationConfiguration()
                config.setTitle("Stratopolis")
                config.setWindowedMode(1600,900)
                config.useVsync(false)
                config.setForegroundFPS(100)
                Lwjgl3Application(appScene, config)
            }
        }
    }
}
