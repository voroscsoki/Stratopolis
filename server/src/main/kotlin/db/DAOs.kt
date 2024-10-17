package dev.voroscsoki.stratopolis.server.db

import api.SerializableWay
import dev.voroscsoki.stratopolis.common.api.Building
import dev.voroscsoki.stratopolis.common.api.SerializableNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class NodeDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NodeDAO>(Nodes)
    var coords by Nodes.coords
    var way by Nodes.way

    fun toNode() = SerializableNode(
        id = id.value,
        coords = coords,
        tags = emptyList(),
    )
}

class WayDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<WayDAO>(Ways)

    val nodes by NodeDAO optionalReferrersOn Nodes.way

    fun toWay() = SerializableWay(
        id = id.value,
        nodes = emptyList(),
        tags = emptyList(),
    )
}

class BuildingDAO(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BuildingDAO>(Buildings)

    var coords by Buildings.coords
    var occupancy by Buildings.occupancy
    var type by Buildings.type

    fun toBuilding() = Building(
        id = id.value,
        coords = coords,
        occupancy = occupancy,
        tags = emptyList(),
        type = type
    )
}