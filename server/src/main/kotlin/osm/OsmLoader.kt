package dev.voroscsoki.stratopolis.server.osm


import java.io.File


class OsmLoader {
    companion object {
        fun read(filename: String) {
            val file = File(filename)
            val unit = OsmStorage(file)
        }
    }
}