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
            //reinitalizeDB()
            simu = Simulation()
            System.gc()
            println("Hello from the server!")

            EngineMain.main(args)
        }

        @JvmStatic
        fun reinitalizeDB() {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.loadFromOsm(storage)
        }
    }
}