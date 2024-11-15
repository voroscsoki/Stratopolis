package dev.voroscsoki.stratopolis.server.db

import dev.voroscsoki.stratopolis.common.util.Vec3
import org.jetbrains.exposed.sql.ColumnType

object Vec3ColumnType : ColumnType<Vec3>() {
    override fun sqlType(): String = "VARCHAR(50)"

    override fun valueFromDB(value: Any): Vec3 {
        return when (value) {
            is String -> Vec3.fromString(value)
            else -> throw IllegalArgumentException("Unexpected value type: $value")
        }
    }

    override fun valueToDB(value: Vec3?): Any {
        return value?.toString() ?: ""
    }

    override fun nonNullValueToString(value: Vec3): String {
        return "'${valueToDB(value)}'"
    }
}