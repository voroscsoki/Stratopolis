package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.SerializableNode
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import kotlin.random.Random
import kotlin.sequences.Sequence

class DatabaseAccess {
    companion object {
        private const val DATABASE_URL = "jdbc:sqlite:test.db"
        fun connect() {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            Database.connect(DATABASE_URL, driver = "org.sqlite.JDBC")

            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    Buildings
                )
            }
        }
        //TODO: roads
        fun loadFromOsm(storage: OsmStorage) {
            transaction {
                println("Wiping old building data")
                Buildings.deleteAll()
            }

            transaction {
                println("Seeding buildings")
                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.type] = building.type
                    this[Buildings.tags] = Yaml.encodeToString(building.tags)
                    this[Buildings.ways] = Yaml.encodeToString(building.ways)
                }
            }
        }

        fun getNodes(baseCoord: Vec3? = null, rangeDegrees: Float? = null): Sequence<SerializableNode> {
            val resultRows = transaction {
                Nodes.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val buildingCoords = row[Nodes.coords]
                val distance = baseCoord?.let { c -> buildingCoords.dist(c) } ?: 0f

                if (rangeDegrees == null || distance <= rangeDegrees) {
                    SerializableNode(
                        row[Nodes.id].value,
                        emptyList(),
                        row[Nodes.coords]
                    )
                } else null
            }
        }

        fun getBuildings(baseCoord: Vec3? = null, range: Float? = null): Sequence<Building> {
            val resultRows = transaction {
                Buildings.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val buildingCoords = row[Buildings.coords]
                val distance = baseCoord?.let { c -> buildingCoords.dist(c) } ?: 0f

                if (range == null || distance <= range) {
                    //TODO: use .toBuilding() (issue is the transaction scope)
                    Building(
                        row[Buildings.id].value,
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = buildingCoords,
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                } else null
            }
        }

        fun getBuildingById(id: Long): Building? {
            return transaction {
                Buildings.selectAll().where(Buildings.id eq id).map { row ->
                    Building(
                        row[Buildings.id].value,
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                }.firstOrNull()
            }
        }

        fun getAverageCoords(): Vec3 {
            return transaction {
                val res = Buildings.select(Buildings.coords).sortedBy { Random.nextFloat() }.map { it[Buildings.coords] }.take(1000)
                return@transaction res.reduce { acc, vec3 -> acc + vec3 } / res.size.toFloat()
            }
        }
    }
}