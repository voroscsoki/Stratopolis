package dev.voroscsoki.stratopolis.server.db

import de.topobyte.osm4j.core.model.iface.EntityType
import dev.voroscsoki.stratopolis.common.api.Vec3
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Nodes : IdTable<Long>("nodes") {
    override val id: Column<EntityID<Long>>
        get() = long("osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val coords = customVec3("coords")
}

object Ways : IdTable<Long>("ways") {
    override val id: Column<EntityID<Long>>
        get() = long("osm_id").entityId()
    override val primaryKey = PrimaryKey(id)
}

object Buildings : IdTable<Long>("buildings") {
    override val id: Column<EntityID<Long>>
        get() = Ways.long("osm_id").entityId()
    override val primaryKey = PrimaryKey(id)

    val coords = customVec3("coords")
    val occupancy = integer("occupancy")
    val type = enumerationByName("type", 10, EntityType::class)
}

fun Table.customVec3(name: String): Column<Vec3> {
    return registerColumn(name, Vec3ColumnType)
}
