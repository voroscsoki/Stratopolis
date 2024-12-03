package dev.voroscsoki.stratopolis.client.networking

import dev.voroscsoki.stratopolis.common.networking.ControlMessage
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class SocketClient(
    val incomingHandler: (ControlMessage) -> Unit,
    val targetAddress: String = "ws://localhost:8085/control"
) {
    private val client: HttpClient = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                classDiscriminator = "type"
            })
        }
    }
    private val sendQueue = Channel<ControlMessage>(Channel.UNLIMITED)
    private val _isConnected = MutableStateFlow(false)
    private val socketScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun listen() {
        try {
            if (!isWebSocketAvailable(targetAddress)) {
                delay(5.seconds)
            }

            client.webSocket(targetAddress) {
                _isConnected.value = true

                coroutineScope {
                    val receiveJob = launch {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val msg = try {
                                    Json.decodeFromString<ControlMessage>(frame.readText())
                                } catch (e: SerializationException) {
                                    null
                                }
                                msg?.let { incomingHandler.invoke(it) }
                            }
                        }
                    }

                    val sendJob = launch {
                        while (isActive) {
                            val item = sendQueue.receive()
                            outgoing.send(Frame.Text(Json.encodeToString(item)))
                        }
                    }

                    joinAll(receiveJob, sendJob)
                }

                _isConnected.value = false
            }
        }
        catch (e: Exception) {
            println("WebSocket connection error: ${e.message}")
        } finally {
            _isConnected.value = false
            disconnect()
        }
    }

    fun disconnect() {
        socketScope.cancel()
        client.close()
        _isConnected.value = false
    }

    suspend fun sendSocketMessage(msg: ControlMessage): Boolean {
        return try {
            if (_isConnected.value) {
                sendQueue.send(msg)
                true
            } else false
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
            false
        }
    }

    suspend fun initializeWebSocket() {
        socketScope.launch { listen() }
        withTimeout(10.seconds) {
            _isConnected.first { it }
        }
    }

    suspend fun isWebSocketAvailable(url: String): Boolean {
        val testClient = HttpClient { install(WebSockets) {} }
        return try {
            testClient.webSocketSession(url) {}
            true
        } catch (e: Exception) {
            println("WebSocket connection failed: ${e.message}")
            false
        } finally {
            testClient.close()
        }
    }

    fun copy(targetAddress: String?): SocketClient {
        return SocketClient(incomingHandler, targetAddress ?: this.targetAddress)
    }
}