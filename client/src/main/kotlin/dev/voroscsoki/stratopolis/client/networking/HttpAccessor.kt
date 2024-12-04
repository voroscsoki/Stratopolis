package dev.voroscsoki.stratopolis.client.networking

import dev.voroscsoki.stratopolis.client.Main
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class HttpAccessor {
    companion object {
        private val client: HttpClient get() = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
            engine {
                config {
                    followRedirects(true)
                }
            }
        }

        fun sendPbfFile(file: File) {
            CoroutineScope(Dispatchers.IO).launch {
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://${Main.socket.basePath.replace("ws://", "")}/pbf_file",
                    formData = formData {
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                )

                if(response.status == HttpStatusCode.OK)
                    Main.instanceData.setupGame()
                client.close()
            }
        }
    }
}