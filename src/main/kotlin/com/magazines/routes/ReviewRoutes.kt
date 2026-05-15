package com.magazines.routes

import com.magazines.data.dto.ReviewCreateRequest
import com.magazines.data.dto.ReviewUpdateRequest
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.model.UserRole
import com.magazines.service.ReviewService
import com.magazines.util.extractUserId
import com.magazines.util.userRoleOrThrow
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.reviewRoutes(reviewService: ReviewService) {
    route("/api/magazines") {
        get("{id}/reviews") {
            val magazineId = call.parseMagazineId()
            call.respond(reviewService.getReviewsByMagazine(magazineId))
        }

        authenticate("auth-jwt") {
            post("{id}/reviews") {
                val magazineId = call.parseMagazineId()
                val userId = call.extractUserId()
                val body = call.receive<ReviewCreateRequest>()
                val dto = reviewService.createReview(magazineId, userId, body)
                call.respond(HttpStatusCode.Created, dto)
            }
        }
    }

    route("/api/reviews") {
        authenticate("auth-jwt") {
            put("{id}") {
                val reviewId = call.parseReviewId()
                val userId = call.extractUserId()
                val body = call.receive<ReviewUpdateRequest>()
                val dto = reviewService.updateReview(reviewId, userId, body)
                call.respond(dto)
            }

            delete("{id}") {
                val reviewId = call.parseReviewId()
                val userId = call.extractUserId()
                val isAdmin = call.userRoleOrThrow() == UserRole.ADMIN
                reviewService.deleteReview(reviewId, userId, isAdmin)
                call.respond(HttpStatusCode.NoContent)
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

private fun io.ktor.server.application.ApplicationCall.parseReviewId(): UUID {
    val raw = parameters["id"] ?: throw BadRequestException("Review id is required")
    return try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid review id: $raw")
    }
}
