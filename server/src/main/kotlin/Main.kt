package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmLoader
import io.ktor.server.netty.*

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the server!")
            OsmLoader.read("hungary-latest.osm.pbf")
            EngineMain.main(args)
        }
    }
}