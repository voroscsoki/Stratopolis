package dev.voroscsoki.stratopolis.client

import dev.voroscsoki.stratopolis.common.api.*

class InstanceData {
    private val handlerFunctions: Map<Class<out ControlMessage>, (ControlMessage) -> Unit> = mapOf(
        NodeResponse::class.java to { msg -> handleNodes(msg as NodeResponse) }
    )
    private val nodes = ObservableMap<Long, SerializableNode>()

    init {
        nodes.listeners.add { change ->
            when (change) {
                is MapChange.Put -> Main.appScene.upsertInstance(change.newVal)
                else -> {}
            }
        }
    }

    fun handleIncomingMessage(msg: ControlMessage) {
        handlerFunctions[msg::class.java]?.invoke(msg)
    }

    private fun handleNodes(msg: NodeResponse) {
        msg.nodes.forEach { nodes += it.id to it }
    }


}