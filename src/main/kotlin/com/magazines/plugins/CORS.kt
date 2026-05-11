package com.magazines.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCORS() {
    install(CORS) {
        allowHost("localhost:8080", schemes = listOf("http"))
        allowHost("localhost:3000", schemes = listOf("http"))
        allowHost("127.0.0.1:8080", schemes = listOf("http"))

        allowHost("10.0.2.2:8080", schemes = listOf("http"))

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        allowNonSimpleContentTypes = true

        allowCredentials = true
    }
}
