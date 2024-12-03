package dev.voroscsoki.stratopolis.client.networking

import dev.voroscsoki.stratopolis.client.Main
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import java.io.File


class HttpAccessor {
    companion object {
        private val client: HttpClient get() = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }
        }

        suspend fun sendPbfFile(file: File) {
            //add json accept header
            client.post("http://${Main.socket.basePath.replace("ws://", "")}/pbf_file") {
                setBody(file.readBytes())
            }
            client.close()
        }
    }
}