/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.client.tests.utils

import io.ktor.client.tests.utils.tests.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.websocket.*

internal fun Application.tests() {
    install(io.ktor.server.websocket.WebSockets) {
        maxFrameSize = 4 * 1024L

        extensions {
            install(WebSocketDeflateExtension)
        }
    }

    authTestServer()
    encodingTestServer()
    serializationTestServer()
    cacheTestServer()
    loggingTestServer()
    contentTestServer()
    fullFormTest()
    redirectTest()
    pluginsTest()
    webSockets()
    multiPartFormDataTest()
    headersTestServer()
    timeoutTest()
    cookiesTest()
    buildersTest()
    downloadTest()
    uploadTest()
    jsonTest()
    multithreadedTest()
    eventsTest()

    routing {
        post("/echo") {
            val response = call.receiveText()
            call.respond(response)
        }
        post("/echo-with-content-type") {
            val response = call.receiveText()
            val contentType = call.request.header(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
            call.respondBytes(response.toByteArray(), contentType)
        }
        get("/bytes") {
            val size = call.request.queryParameters["size"]!!.toInt()
            call.respondBytes(makeArray(size))
        }
        post("/content-type") {
            val contentType = call.request.header(HttpHeaders.ContentType)
            call.respondText(contentType ?: "")
        }
    }
}

internal fun Application.tlsTests() {
    install(DefaultHeaders) {
        header("X-Comment", "TLS test server")
    }

    routing {
        get("/") {
            call.respondText("Hello, TLS!")
        }
    }
}

internal suspend fun ApplicationCall.fail(text: String): Nothing {
    respondText(text, status = HttpStatusCode(400, text))
    error(text)
}
