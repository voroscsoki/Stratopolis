package db

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getNodeAverage
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Roads
import dev.voroscsoki.stratopolis.server.osm.OsmStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.Connection
import kotlin.sequences.Sequence

class DatabaseAccess {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private const val DATABASE_URL = "jdbc:sqlite:test.db"
        fun connect() {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            Database.connect(DATABASE_URL, driver = "org.sqlite.JDBC")

            transaction {
                SchemaUtils.createMissingTablesAndColumns(
                    Buildings, Roads
                )
            }
        }

        fun reinitalizeDB(file: ByteArray) {
            val storage = OsmStorage(file)
            loadFromOsm(storage)
        }

        private fun loadFromOsm(storage: OsmStorage) {
            transaction {
                logger.warn("Wiping old data")
                Buildings.deleteAll()
                Roads.deleteAll()
            }

            transaction {
                logger.info("Seeding buildings")
                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.type] = building.osmType
                    this[Buildings.tags] = Json.encodeToString(building.tags)
                    this[Buildings.ways] = Json.encodeToString(building.ways)
                    this[Buildings.capacity] = building.capacity
                }
            }

            transaction {
                logger.info("Seeding roads")
                Roads.batchUpsert(storage.roads, Roads.id) { road ->
                    this[Roads.id] = road.id
                    this[Roads.tags] = Json.encodeToString(road.tags)
                    this[Roads.ways] = Json.encodeToString(road.ways)
                }
            }
            logger.info("DB rebuilt")
        }

        fun getBuildings(baseCoord: Vec3? = null, range: Double? = null): Sequence<Building> {
            val resultRows = transaction {
                Buildings.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val buildingCoords = row[Buildings.coords]
                val distance = baseCoord?.let { c -> buildingCoords.dist(c) } ?: 0.0

                if (range == null || distance <= range) {
                    //TODO: use .toBuilding() (issue is the transaction scope)
                    Building(
                        row[Buildings.id].value,
                        Json.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = buildingCoords,
                        ways = Json.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                        capacity = row[Buildings.capacity]
                    )
                } else null
            }
        }

        fun getRoads(baseCoord: Vec3? = null, range: Double? = null): Sequence<Road> {
            val resultRows = transaction {
                Roads.selectAll().iterator().asSequence()
            }

            return resultRows.mapNotNull { row ->
                val output = row.let {
                    Road(
                        it[Roads.id].value,
                        Json.decodeFromString<List<SerializableTag>>(it[Roads.tags]),
                        Json.decodeFromString<List<SerializableWay>>(it[Roads.ways])
                    )
                }
                val coords = output.ways.flatMap { it.nodes }.getNodeAverage()
                val distance = baseCoord?.let { c -> coords.dist(c) } ?: 0.0

                if (range == null || distance <= range) {
                    output
                } else null
            }
        }

        fun getBuildingById(id: Long): Building? {
            return transaction {
                Buildings.selectAll().where(Buildings.id eq id).map { row ->
                    Building(
                        row[Buildings.id].value,
                        Json.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Json.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                        capacity = row[Buildings.capacity]
                    )
                }.firstOrNull()
            }
        }

        fun getAverageCoords(): Vec3 {
            val count = 100
            return transaction {
                val res = Buildings.select(Buildings.coords).shuffled().map { it[Buildings.coords] }.take(count)
                if(res.isEmpty()) return@transaction Vec3(0.0, 0.0, 0.0)
                return@transaction res.reduce { acc, vec3 -> acc + vec3 } / res.size.toFloat()
            }
        }


        fun getBuildingsByType(type: String, count: Int): List<Building> {
            return transaction {
                Buildings.selectAll().where { Buildings.tags like "%$type%" }.shuffled().take(count).map { row ->
                    Building(
                        row[Buildings.id].value,
                        Json.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Json.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                }
            }
        }
    }
}