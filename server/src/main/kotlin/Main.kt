package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import io.ktor.server.netty.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.batchUpsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val storage = OsmStorage(File("budapest.osm.pbf"))
            DatabaseAccess.connect()
            println(storage.nodes.size)
            transaction {
                Nodes.batchUpsert(storage.nodes.map { SerializableNode(it.value) }, Nodes.id) { node ->
                    this[Nodes.id] = EntityID(node.id, Nodes)
                    this[Nodes.coords] = node.coords
                }
            }
                println("Hello from the server!")
                EngineMain.main(args)
            }
    }
    }