package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.api.HttpAccessor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            runBlocking {
                asyncInit()
            }
        }
        private suspend fun asyncInit() {
            println("Hello from the client!")
            coroutineScope {
                println("The client understands: ${HttpAccessor.testRequest()}")

            }
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Drop")
            config.setWindowedMode(600, 480)
            config.useVsync(true)
            config.setForegroundFPS(60)
            Lwjgl3Application(Basic3D(), config)
        }
    }
}
