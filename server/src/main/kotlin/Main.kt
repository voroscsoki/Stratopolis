package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.connect()
            println(storage.nodes.size)
            println("Hello from the server!")
            EngineMain.main(args)
        }
    }
}