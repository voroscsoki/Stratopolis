package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    routing {
        staticResources("static", "static")
        // Define a route at /test
        //send the json representation of the Building object
        get("/test") {
            val bldg = OsmStorage.buildings
            call.respond(bldg)
        }

        get("/health-check") {
            call.respond("OK")
        }
    }
}