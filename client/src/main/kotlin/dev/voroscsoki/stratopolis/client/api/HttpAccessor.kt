package dev.voroscsoki.stratopolis.client.api
import java.net.URI
import java.net.URL
import java.net.http.HttpClient

class HttpAccessor {
    companion object {

        fun testRequest() : String {
            return URI("http://localhost:8085/test").toURL().readText().toString()
        }
    }
}