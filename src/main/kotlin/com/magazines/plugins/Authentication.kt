package com.magazines.plugins

import com.magazines.config.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject

fun Application.configureAuthentication() {
    val jwtConfig by inject<JwtConfig>()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier())
            validate { credential ->
                val sub = credential.payload.subject
                val email = credential.payload.getClaim("email").asString()
                if (!sub.isNullOrBlank() && !email.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or missing token"))
            }
        }
    }
}
