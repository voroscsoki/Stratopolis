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
        var socket = SocketClient(instanceData::handleIncomingMessage, "ws://localhost:8085/controll")

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                if(socket.isWebSocketAvailable(socket.targetAddress)) socket.initializeWebSocket()
                val config = Lwjgl3ApplicationConfiguration()
                config.setTitle("Stratopolis")
                config.setWindowedMode(3840, 2160)
                config.useVsync(true)
                config.setForegroundFPS(500)
                Lwjgl3Application(appScene, config)
            }
        }
    }
}
