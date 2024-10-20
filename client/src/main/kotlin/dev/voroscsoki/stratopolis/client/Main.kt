package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.api.HttpAccessor
import dev.voroscsoki.stratopolis.common.api.OsmLoadRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Main {
    companion object {
        val appScene = Basic3D()
        val instanceData = InstanceData()
        val socket = SocketClient()

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                launch { socket.listenOnSocket() }
                Thread.sleep(500)
                launch { socket.sendSocketMessage(OsmLoadRequest("budapest.osm.pbf")) }
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
