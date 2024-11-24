package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getAverage
import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Roads
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
                    Buildings, Roads
                )
            }
        }
        //TODO: roads
        fun loadFromOsm(storage: OsmStorage) {
            transaction {
                println("Wiping old data")
                Buildings.deleteAll()
                Roads.deleteAll()
            }

            transaction {
                //TODO: logger
                println("Seeding buildings")
                Buildings.batchUpsert(storage.buildings, Buildings.id) { building ->
                    this[Buildings.id] = building.id
                    this[Buildings.coords] = building.coords
                    this[Buildings.type] = building.type
                    this[Buildings.tags] = Yaml.encodeToString(building.tags)
                    this[Buildings.ways] = Yaml.encodeToString(building.ways)
                }
            }

            transaction {
                println("Seeding roads")
                Roads.batchUpsert(storage.roads, Roads.id) { road ->
                    this[Roads.id] = road.id
                    this[Roads.tags] = Yaml.encodeToString(road.tags)
                    this[Roads.ways] = Yaml.encodeToString(road.ways)
                }
            }
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
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = buildingCoords,
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
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
                        Yaml.decodeFromString<List<SerializableTag>>(it[Roads.tags]),
                        Yaml.decodeFromString<List<SerializableWay>>(it[Roads.ways])
                    )
                }
                val coords = output.ways.flatMap { it.nodes }.getAverage()
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

        fun getRandomBuildings(count: Int, origin: Vec3? = null): List<Building> {
            return transaction {
                val ids = Buildings.selectAll().map { it[Buildings.id].value }.shuffled().take(count)
                return@transaction Buildings.selectAll().where { Buildings.id inList ids }.map { row ->
                    Building(
                        row[Buildings.id].value,
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        type = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                }
            }
        }
    }
}