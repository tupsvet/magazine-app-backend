package com.magazines.routes

import com.magazines.data.dto.RejectRequest
import com.magazines.domain.exception.BadRequestException
import com.magazines.service.MagazineService
import com.magazines.service.ModerationService
import com.magazines.util.requireAdmin
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.adminRoutes(
    moderationService: ModerationService,
    magazineService: MagazineService,
) {
    route("/api/admin/magazines") {
        authenticate("auth-jwt") {
            get("/pending") {
                call.requireAdmin()
                val pending = moderationService.listPending()
                call.respond(magazineService.toMagazineDtos(pending))
            }

            post("/{id}/approve") {
                call.requireAdmin()
                val magazineId = call.parseMagazineId()
                val magazine = moderationService.approve(magazineId)
                call.respond(magazineService.toMagazineDto(magazine))
            }

            post("/{id}/reject") {
                call.requireAdmin()
                val magazineId = call.parseMagazineId()
                val body = call.receive<RejectRequest>()
                val magazine = moderationService.reject(magazineId, body.reason)
                call.respond(magazineService.toMagazineDto(magazine))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.parseMagazineId(): UUID {
    val raw = parameters["id"] ?: throw BadRequestException("Magazine id is required")
    return try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid magazine id: $raw")
    }
}
