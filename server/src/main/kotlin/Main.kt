package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmReader
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.netty.*

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the server!")
            OsmReader.read("hungary-latest.osm.pbf")
            EngineMain.main(args)
        }
    }
}
    
fun Application.configureRouting() {
    routing {
        staticResources("static", "static")

        get("/test") {
            call.respond("Hello from the server!\nWe have ${OsmStorage.buildings.size} buildings. Here's one!\n${OsmStorage.buildings.first()}")
        }
    }
}