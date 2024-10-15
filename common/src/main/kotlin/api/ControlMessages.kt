package dev.voroscsoki.stratopolis.common.api
import kotlinx.serialization.Serializable

@Serializable
sealed class ControlMessage

@Serializable
class EchoReq(val msg: String) : ControlMessage()

@Serializable
class EchoResp(val msg: String) : ControlMessage()