package dev.voroscsoki.stratopolis.client.api

import dev.voroscsoki.stratopolis.common.api.Building
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class HttpAccessor {
    companion object {
        private val client: HttpClient get() = HttpClient(OkHttp) {
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
                        Thread.sleep(2000)
                    }
                }
            }
        }

        suspend fun testRequest(): List<Building> {
            //add json accept header
            val httpResponse = client.get("http://localhost:8085/test")
            client.close()
            return Json.decodeFromString(httpResponse.body())
        }
    }
}