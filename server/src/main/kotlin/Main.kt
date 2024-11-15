package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.api.Simulation
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        val socketServer = SocketServer()
        lateinit var simu : Simulation

        @JvmStatic
        fun main(args: Array<String>) {
            DatabaseAccess.connect()
            println("Hello from the server!")
            //reinitalizeDB()
            simu = Simulation()

            EngineMain.main(args)
        }

        @JvmStatic
        fun reinitalizeDB() {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.seedFromOsm(storage)
        }
    }
}