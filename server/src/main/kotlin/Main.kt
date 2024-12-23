package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.networking.SocketServer
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import dev.voroscsoki.stratopolis.server.osm.Simulation
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        val socketServer = SocketServer()
        lateinit var simu : Simulation

        @JvmStatic
        fun main(args: Array<String>) {
            DatabaseAccess.connect()
            reinitalizeDB()
            println("Hello from the server!")
            //reinitalizeDB()
            //simu = Simulation()

            EngineMain.main(args)
        }

        @JvmStatic
        fun reinitalizeDB() {
            val storage = OsmStorage(File("Wien.osm.pbf"))
            DatabaseAccess.seedFromOsm(storage)
        }
    }
}