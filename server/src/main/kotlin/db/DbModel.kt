package dev.voroscsoki.stratopolis.server.db

import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.util.Vec3
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


object Nodes : IdTable<Long>("nodes") {
    override val id: Column<EntityID<Long>> = long("osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val coords = customVec3("coords").default(Vec3(0f, 0f, 0f))
    val way = reference("wayId", Ways).nullable()
    val tags = text("tags")
}

object Ways : IdTable<Long>("ways") {
    override val id: Column<EntityID<Long>> = long("osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val tags = text("tags")
    //val nodes by NodeDAO referrersOn Nodes.way
}

object Buildings : IdTable<Long>("buildings") {
    override val id: Column<EntityID<Long>> = long("building_osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val coords = customVec3("coords")
    val occupancy = integer("occupancy")
    val type = enumeration<EntityType>("type").default(EntityType.Node)
    val tags = Buildings.text("tags")
    val ways = Buildings.text("ways")
}

fun Table.customVec3(name: String): Column<Vec3> {
    return registerColumn(name, Vec3ColumnType)
}
