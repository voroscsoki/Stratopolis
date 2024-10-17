package dev.voroscsoki.stratopolis.server

import api.SerializableWay
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.server.db.dbUpsert
import dev.voroscsoki.stratopolis.server.db.overwriteTable
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.connect()
            var i = 0
            println(storage.nodes.size)
            transaction {
                storage.nodes.values.map { SerializableNode(it) }.overwriteTable()
                storage.ways.values.map { SerializableWay(it) }.overwriteTable()
                storage.buildings.forEach { it.dbUpsert()}
            }
            println("Hello from the server!")
            EngineMain.main(args)
        }
    }
}