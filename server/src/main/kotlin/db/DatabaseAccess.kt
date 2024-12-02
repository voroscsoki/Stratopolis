package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.common.elements.Building
import dev.voroscsoki.stratopolis.common.elements.Road
import dev.voroscsoki.stratopolis.common.elements.SerializableTag
import dev.voroscsoki.stratopolis.common.elements.SerializableWay
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getNodeAverage
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
                    this[Buildings.type] = building.osmType
                    this[Buildings.tags] = Yaml.encodeToString(building.tags)
                    this[Buildings.ways] = Yaml.encodeToString(building.ways)
                    this[Buildings.capacity] = building.capacity()
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
                        osmType = row[Buildings.type],
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
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                }.firstOrNull()
            }
        }

        fun getAverageCoords(): Vec3 {
            val count = 100
            return transaction {
                val res = Buildings.select(Buildings.coords).shuffled().map { it[Buildings.coords] }.take(count)
                return@transaction res.reduce { acc, vec3 -> acc + vec3 } / res.size.toFloat()
            }
        }

        fun getRandomBuildings(count: Int, origin: Vec3? = null, type: String? = null): List<Building> {
            return transaction {
                // Generate the base query
                val query = if (type != null) {
                    // Filter by type in the database (assuming tags is stored as JSON or text)
                    // Adjust for your DB syntax
                    Buildings.selectAll().where { // Adjust for your DB syntax
                        (Buildings.tags like "%\"building\":\"$type\"%") // Adjust for your DB syntax
                    }
                } else {
                    Buildings.selectAll()
                }

                // Randomize and limit the result in the database
                val randomizedRows = query.shuffled().take(count).toList()

                // Decode and map only the rows needed
                randomizedRows.map { row ->
                    Building(
                        row[Buildings.id].value,
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways])
                    )
                }
            }
        }



        fun getBuildingsByType(type: String, location: Vec3): List<Building> {
            return transaction {
                Buildings.selectAll().where { Buildings.tags like "%$type%" }.map { row ->
                    Building(
                        row[Buildings.id].value,
                        Yaml.decodeFromString<List<SerializableTag>>(row[Buildings.tags]),
                        osmType = row[Buildings.type],
                        coords = row[Buildings.coords],
                        ways = Yaml.decodeFromString<List<SerializableWay>>(row[Buildings.ways]),
                    )
                }
            }
        }
    }
}