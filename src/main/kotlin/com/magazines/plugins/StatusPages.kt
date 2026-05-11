package com.magazines.plugins

import com.magazines.domain.exception.EmailAlreadyTakenException
import com.magazines.domain.exception.InvalidCredentialsException
import com.magazines.domain.exception.UserNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<EmailAlreadyTakenException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to (cause.message ?: "Email already taken")))
        }
        exception<InvalidCredentialsException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
        }
        exception<UserNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (cause.message ?: "User not found")))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: cause::class.simpleName ?: "Internal error")),
            )
        }
    }
}
