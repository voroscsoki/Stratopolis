package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.api.HttpAccessor
import kotlinx.coroutines.runBlocking

class Main {
    companion object {
        val appScene = Basic3D()
        val socket = SocketClient()

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                socket.testEcho()
                asyncInit()
            }
        }
        private suspend fun asyncInit() {
            HttpAccessor.waitForConnection()
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Stratopolis")
            config.setWindowedMode(1280, 720)
            config.useVsync(false)
            config.setForegroundFPS(60)
            Lwjgl3Application(appScene, config)
        }
    }
}
