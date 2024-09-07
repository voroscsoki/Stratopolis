package dev.voroscsoki.stratopolis.client.api

import dev.voroscsoki.stratopolis.common.api.Building
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class HttpAccessor {
    companion object {
        private val client = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }
        }

        suspend fun waitForConnection() {
            while (true) {
                try {
                    if(client.get("http://localhost:8085/health-check").status.value == 200) break
                } catch (e: Exception) {
                    println("Waiting for server to start...")
                    withContext(Dispatchers.IO) {
                        Thread.sleep(5000)
                    }
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