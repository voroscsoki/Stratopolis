package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.DatabaseAccess.Companion.reinitalizeDB
import dev.voroscsoki.stratopolis.server.networking.SocketServer
import dev.voroscsoki.stratopolis.server.networking.configureRouting
import dev.voroscsoki.stratopolis.server.osm.Simulation
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import java.io.File

class Main {
    companion object {
        private val configuration =
            try {
                val file = File("config.json")
                Json.decodeFromString<Config>(file.readText())
            } catch (_: Exception) {
                Config()
            }
        val socketServer = SocketServer()
        lateinit var simu : Simulation

        @JvmStatic
        fun main(args: Array<String>) {
            DatabaseAccess.connect()
            reinitalizeDB(File("Wien.osm.pbf").readBytes())
            simu = Simulation()
            System.gc()
            println("Hello from the server!")
            embeddedServer(Netty, port = configuration.port) {
                configureRouting()
            }.start(wait = true)
        }

    }
}