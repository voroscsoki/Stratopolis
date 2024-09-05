package dev.voroscsoki.stratopolis.server


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello from the server!")
            /*val connection = OsmConnection("https://overpass-api.de/api/", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0")
            val overpass = OverpassMapDataApi(connection)
            val handler = SimpleQueryHandler()
            //query for Burger King in Budapest
            overpass.queryElements(
                "node[\"name\"=\"Burger King\"](47.5,19.0,47.6,19.1);out;",
                handler
            )*/
            OsmReader.read("hungary-latest.osm.pbf")
        }
    }
}