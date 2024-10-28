package dev.voroscsoki.stratopolis.server

import api.SerializableWay
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import dev.voroscsoki.stratopolis.common.api.Vec3
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.db.Ways
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import kotlin.sequences.Sequence

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

        fun seedFromOsm(storage: OsmStorage) {
            storage.nodes.values.chunked(200000).forEach { chunk ->
                println("Seeding chunk of nodes")
                transaction {
                    Nodes.batchUpsert(chunk.map { SerializableNode(it) }, Nodes.id) { node ->
                        this[Nodes.id] = EntityID(node.id, Nodes)
                        this[Nodes.coords] = node.coords
                        this[Nodes.tags] = Json.encodeToString(node.tags)
                    }
                }
            }
            println("Seeding ways")
            transaction {
                val allWays = storage.ways.map { SerializableWay(it.value) }
                Ways.batchUpsert(allWays, Ways.id) { way ->
                    this[Ways.id] = EntityID(way.id, Ways)
                    this[Ways.tags] = Json.encodeToString(way.tags)
                }
                allWays.forEach { way ->
                    way.nodes.forEach { node ->
                        Nodes.update({ Nodes.id eq node }) {
                            //update the way column
                            it[Nodes.way] = EntityID(way.id, Ways)
                        }
                    }
                }

                /*println("Seeding buildings")
                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.occupancy] = 0
                    this[Buildings.type] = building.type
                }
            }*/
            }
        }


            fun getNodes(baseCoord: Vec3? = null): Sequence<SerializableNode> {
                val resultRows = transaction {
                    Nodes.selectAll().iterator().asSequence()
                }

                return resultRows.mapNotNull { row ->
                    val buildingCoords = row[Nodes.coords]
                    val distance = baseCoord?.let { c -> buildingCoords - c } ?: 0.0

                    if (distance <= 0.005) {
                        SerializableNode(
                            row[Nodes.id].value,
                            emptyList(),
                            row[Nodes.coords]
                        )
                    } else null
                }
            }
        }
    }