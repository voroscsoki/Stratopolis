package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.nodeIds
import dev.voroscsoki.stratopolis.common.util.tags
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.db.Ways
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
                Ways.batchUpsert(storage.ways.values, Ways.id) { way ->
                    this[Ways.id] = EntityID(way.id, Ways)
                    this[Ways.tags] = Json.encodeToString(way.tags.map { SerializableTag(it) })
                }
                storage.ways.values.forEach { way ->
                    way.nodeIds.forEach { node ->
                        Nodes.update({ Nodes.id eq node }) {
                            //update the way column
                            it[Nodes.way] = EntityID(way.id, Ways)
                        }
                    }
                }

                println("Seeding buildings")
                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.occupancy] = 0
                    this[Buildings.type] = building.type
                    this[Buildings.tags] = Json.encodeToString(building.tags)
                    this[Buildings.ways] = Json.encodeToString(building.ways)
                }
            }
        }

        fun getNodes(baseCoord: Vec3? = null, rangeDegrees: Float? = null): Sequence<SerializableNode> {
            val resultRows = transaction {
                Nodes.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val buildingCoords = row[Nodes.coords]
                val distance = baseCoord?.let { c -> buildingCoords.dist(c) } ?: 0.0

                if (rangeDegrees == null || distance <= rangeDegrees) {
                    SerializableNode(
                        row[Nodes.id].value,
                        emptyList(),
                        row[Nodes.coords]
                    )
                } else null
            }
        }

        fun getBuildings(baseCoord: Vec3? = null, rangeDegrees: Float? = null): Sequence<Building> {
            val resultRows = transaction {
                Buildings.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val buildingCoords = row[Buildings.coords]
                val distance = baseCoord?.let { c -> buildingCoords.dist(c) } ?: 0.0

                if (rangeDegrees == null || distance <= rangeDegrees) {
                    //TODO: use .toBuilding() (issue is the transaction scope)
                    Building(
                        row[Buildings.id].value,
                        Json.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = buildingCoords,
                        ways = Json.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                        occupancy = row[Buildings.occupancy]
                    )
                } else null
            }
        }

        fun getBuildingById(id: Long): Building? {
            return transaction {
                Buildings.selectAll().where(Buildings.id eq id).map { row ->
                    Building(
                        row[Buildings.id].value,
                        Json.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Json.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                        occupancy = row[Buildings.occupancy]
                    )
                }.firstOrNull()
            }
        }
    }
}