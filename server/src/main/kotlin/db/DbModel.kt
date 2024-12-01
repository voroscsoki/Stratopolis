package dev.voroscsoki.stratopolis.server.db

import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.server.db.Buildings.default
import dev.voroscsoki.stratopolis.server.db.Buildings.entityId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


object Buildings : IdTable<Long>("buildings") {
    override val id: Column<EntityID<Long>> = long("building_osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val coords = customVec3("coords")
    val type = enumeration<EntityType>("type").default(EntityType.Node)
    val tags = Buildings.text("tags")
    val ways = Buildings.text("ways")
    val capacity = Buildings.uinteger("capacity")
}

object Roads : IdTable<Long>("roads") {
    override val id: Column<EntityID<Long>> = Roads.long("road_osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val tags = Roads.text("tags")
    val ways = Roads.text("ways")
}

fun Table.customVec3(name: String): Column<Vec3> {
    return registerColumn(name, Vec3ColumnType)
}
