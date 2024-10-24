package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        val socketServer = SocketServer()

        @JvmStatic
        fun main(args: Array<String>) {
            var storage: OsmStorage? = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.connect()
            println(storage?.nodes?.size)
            println("Hello from the server!")
            //HACK
            storage = null
            System.gc()
            EngineMain.main(args)
        }
    }
}