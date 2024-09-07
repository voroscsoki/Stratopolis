package dev.voroscsoki.stratopolis.client.api

import dev.voroscsoki.stratopolis.common.api.Building
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json


class HttpAccessor {
    companion object {
        val client = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }
        }

        suspend fun testRequest(): String {
            //add json accept header
            val httpResponse = client.get("http://localhost:8085/test") {
                headers {
                    append("Accept", "application/json")
                }
            }
            if (httpResponse.status.value in 200..299) {
                println("Successful response!")
            }
            client.close()
            val txt = httpResponse.body<String>()
            return Json.decodeFromString<List<Building>>(txt).toString()
        }
    }
}