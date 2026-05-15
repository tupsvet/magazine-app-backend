package com.magazines.routes

import com.magazines.domain.exception.BadRequestException
import com.magazines.service.FavoriteService
import com.magazines.util.extractUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.favoriteRoutes(favoriteService: FavoriteService) {
    route("/api/users/me/favorites") {
        authenticate("auth-jwt") {
            get {
                val userId = call.extractUserId()
                call.respond(favoriteService.getUserFavorites(userId))
            }

            post("{magazineId}") {
                val userId = call.extractUserId()
                val magazineId = call.parseMagazineIdParam()
                val added = favoriteService.addToFavorites(userId, magazineId)
                val dto = favoriteService.getMagazineDto(magazineId)
                if (added) {
                    call.respond(HttpStatusCode.Created, dto)
                } else {
                    call.respond(HttpStatusCode.OK, dto)
                }
            }

            delete("{magazineId}") {
                val userId = call.extractUserId()
                val magazineId = call.parseMagazineIdParam()
                favoriteService.removeFromFavorites(userId, magazineId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.parseMagazineIdParam(): UUID {
    val raw = parameters["magazineId"] ?: throw BadRequestException("Magazine id is required")
    return try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid magazine id: $raw")
    }
}
