package com.magazines.plugins

import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.ConflictException
import com.magazines.domain.exception.ForbiddenException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.exception.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException

private val logger = LoggerFactory.getLogger("StatusPages")

private fun errorBody(error: String, message: String): Map<String, String> =
    mapOf("error" to error, "message" to message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                errorBody("NotFound", cause.message ?: "Resource not found"),
            )
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                errorBody("Forbidden", cause.message ?: "Forbidden"),
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                errorBody("BadRequest", cause.message ?: "Bad request"),
            )
        }
        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                errorBody("Conflict", cause.message ?: "Conflict"),
            )
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                errorBody("Unauthorized", cause.message ?: "Unauthorized"),
            )
        }

        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                errorBody("Conflict", cause.message ?: "Email already taken"),
            )
        }
        exception<UnauthorizedException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                errorBody("Unauthorized", "Invalid email or password"),
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                errorBody("NotFound", cause.message ?: "User not found"),
            )
        }
        exception<KtorBadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                errorBody("BadRequest", cause.message ?: "Bad request"),
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                errorBody("InternalServerError", "Internal server error"),
            )
        }
    }
}
