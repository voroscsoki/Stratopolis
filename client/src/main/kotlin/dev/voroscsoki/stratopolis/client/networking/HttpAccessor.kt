package dev.voroscsoki.stratopolis.client.networking

import dev.voroscsoki.stratopolis.client.Main
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://${Main.socket.basePath.replace("ws://", "")}/pbf_file",
                formData = formData {
                    append("file", file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    })
                }
            )

            // Print the server response
            println("Response: ${response.status}")
            println("Response content: ${response.bodyAsText()}")
            client.close()
        }
    }
}