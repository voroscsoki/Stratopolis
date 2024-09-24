package dev.voroscsoki.stratopolis.server.osm


import de.topobyte.osm4j.pbf.seq.PbfIterator
import java.io.File


class OsmLoader {
    companion object {
        fun read(filename: String) {
            val fileStream = File(filename).inputStream()

            val osmIterator = PbfIterator(fileStream, true)
            val unit = OsmStorage(osmIterator)
        }
    }
}