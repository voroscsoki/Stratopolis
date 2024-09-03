package dev.voroscsoki.stratopolis.client
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the client!")
            val config = Lwjgl3ApplicationConfiguration()
            config.setTitle("Drop")
            config.setWindowedMode(1600, 900)
            config.useVsync(true)
            config.setForegroundFPS(60)
            Lwjgl3Application(Basic3D(), config)
        }
    }
}
