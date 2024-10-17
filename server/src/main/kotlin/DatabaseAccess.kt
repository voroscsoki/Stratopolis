package dev.voroscsoki.stratopolis.server

import dev.voroscsoki.stratopolis.server.db.Buildings
import dev.voroscsoki.stratopolis.server.db.Nodes
import dev.voroscsoki.stratopolis.server.db.WayNodeLinkages
import dev.voroscsoki.stratopolis.server.db.Ways
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAccess {
    companion object {
        private const val DATABASE_URL = "jdbc:sqlite:test.db"
        fun connect() {
            Database.connect(DATABASE_URL, driver = "org.sqlite.JDBC")

            transaction {
                SchemaUtils.drop(Buildings, Nodes, Ways, WayNodeLinkages)
                SchemaUtils.create(Buildings, Nodes, Ways, WayNodeLinkages)
            }
        }
    }

}