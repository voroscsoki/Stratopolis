package dev.voroscsoki.stratopolis.common.api
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@Polymorphic
sealed class ControlMessage {
    @Serializable
    sealed class ClientMessage : ControlMessage() {
        @Serializable
        @SerialName("EchoRequestEvent")
        data object EchoRequestEvent : ClientMessage() {
            val text = "EchoRequestEvent"
        }
    }

    @Serializable
    sealed class ServerMessage : ControlMessage() {
        @Serializable
        @SerialName("EchoResponseEvent")
        data class EchoResponseEvent(val text: String) : ServerMessage()
    }
}

val controlMessageModule = SerializersModule {
    polymorphic(ControlMessage::class) {
        subclass(ControlMessage.ClientMessage.EchoRequestEvent::class)
        subclass(ControlMessage.ServerMessage.EchoResponseEvent::class)
    }
}