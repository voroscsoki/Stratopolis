package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        val socketServer = SocketServer()

        @JvmStatic
        fun main(args: Array<String>) {
            DatabaseAccess.connect()
            //reinitalizeDB()
            println("Hello from the server!")
            EngineMain.main(args)
        }

        @JvmStatic
        fun reinitalizeDB() {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.seedFromOsm(storage)
        }
    }
}