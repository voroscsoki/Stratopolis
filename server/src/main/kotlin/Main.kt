package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.Agent
import dev.voroscsoki.stratopolis.common.api.AgentSimulation
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import java.io.File

class Main {
    companion object {
        val socketServer = SocketServer()
        lateinit var simu : AgentSimulation

        @JvmStatic
        fun main(args: Array<String>) {
            DatabaseAccess.connect()
            println("Hello from the server!")
            //reinitalizeDB()
            val bldg1 = DatabaseAccess.getBuildingById(776062316)
            val bldg2 = DatabaseAccess.getBuildingById(82478999)
            simu = AgentSimulation(Agent(1, bldg1!!, bldg2!!, bldg1.coords))

            EngineMain.main(args)
        }

        @JvmStatic
        fun reinitalizeDB() {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.seedFromOsm(storage)
        }
    }
}