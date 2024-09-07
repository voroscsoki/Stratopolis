package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import dev.voroscsoki.stratopolis.client.api.HttpAccessor

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            println("The client understands: ${HttpAccessor.testRequest()}")
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Drop")
            config.setWindowedMode(1600, 900)
            config.useVsync(true)
            config.setForegroundFPS(60)
            Lwjgl3Application(Basic3D(), config)
        }
    }
}
