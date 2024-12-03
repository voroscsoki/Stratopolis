package dev.voroscsoki.stratopolis.server

import kotlinx.serialization.Serializable

@Serializable
data class Config(val port: Int = 8085)