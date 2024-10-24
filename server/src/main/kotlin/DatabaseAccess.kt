package dev.voroscsoki.stratopolis.server

import api.SerializableWay
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.common.api.Vec3
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.db.Ways
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

class DatabaseAccess {
    companion object {
        private const val DATABASE_URL = "jdbc:sqlite:test.db"
        fun connect() {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            Database.connect(DATABASE_URL, driver = "org.sqlite.JDBC")

            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    Nodes, Ways, Buildings
                )
            }
        }

        fun seedFromOsm(file: File) {
            val storage = OsmStorage(file)
            transaction {
                Nodes.batchUpsert(storage.nodes.map { SerializableNode(it.value) }, Nodes.id) { node ->
                    this[Nodes.id] = EntityID(node.id, Nodes)
                    this[Nodes.coords] = node.coords
                }

                val allWays = storage.ways.map { SerializableWay(it.value) }
                Ways.batchUpsert(allWays, Ways.id) { way ->
                    this[Ways.id] = EntityID(way.id, Ways)
                }
                allWays.forEach { way ->
                    way.nodes.forEach { node ->
                        Nodes.update({ Nodes.id eq node }) {
                            //update the way column
                            it[Nodes.way] = EntityID(way.id, Ways)
                        }
                    }
                }

                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.occupancy] = 0
                    this[Buildings.type] = building.type
                }
            }
        }

        fun getNodes(baseCoord: Vec3? = null): List<SerializableNode> {
            return transaction {
                val inter = Nodes.selectAll()
                    .mapNotNull {
                        val buildingCoords = it[Nodes.coords]
                        baseCoord ?: return@mapNotNull it[Nodes.id]
                        val distance = buildingCoords - baseCoord
                        if (distance <= 0.005) { it[Nodes.id] } else { null }
                    }
                println(inter.count())
                return@transaction Nodes.selectAll().where { Nodes.id inList inter }.map {
                    SerializableNode(
                        it[Nodes.id].value,
                        emptyList(),
                        it[Nodes.coords]
                    )
                }
            }
        }
    }
}