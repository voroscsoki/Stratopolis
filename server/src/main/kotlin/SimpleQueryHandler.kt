package dev.voroscsoki.stratopolis.server

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.Node
import de.westnordost.osmapi.map.data.Relation
import de.westnordost.osmapi.map.data.Way
import de.westnordost.osmapi.map.handler.MapDataHandler

class SimpleQueryHandler : MapDataHandler {
    override fun handle(p0: BoundingBox?) {
        println("BoundingBox: $p0")
    }

    override fun handle(p0: Node?) {
        println("Node: ${p0?.tags?.values}")
    }

    override fun handle(p0: Way?) {
        TODO("Not yet implemented")
    }

    override fun handle(p0: Relation?) {
        TODO("Not yet implemented")
    }

}