package com.magazines.routes

import com.magazines.data.dto.MagazineCreateRequest
import com.magazines.data.dto.MagazineUpdateRequest
import com.magazines.domain.exception.BadRequestException
import com.magazines.service.AuthService
import com.magazines.service.MagazineService
import com.magazines.util.currentUser
import com.magazines.util.userIdOrThrow
import com.magazines.util.userRoleOrThrow
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.magazineRoutes(
    magazineService: MagazineService,
    authService: AuthService,
) {
    route("/api/magazines") {

        get {
            val category = call.request.queryParameters["category"]?.toIntOrNull()
            val search = call.request.queryParameters["search"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val result = magazineService.listPublic(category, search, page, pageSize)
            call.respond(result)
        }

        authenticate("auth-jwt") {
            get("/mine") {
                val user = call.currentUser(authService)
                call.respond(magazineService.listMine(user.id))
            }
        }

        authenticate("auth-jwt", optional = true) {
            get("/{id}") {
                val id = call.parseId()
                val principal = call.principal<JWTPrincipal>()
                val user = if (principal != null) call.currentUser(authService) else null
                val dto = magazineService.getPublicMagazine(id, user)
                call.respond(dto)
            }
        }

        authenticate("auth-jwt") {
            post {
                val user = call.currentUser(authService)
                val body = call.receive<MagazineCreateRequest>()
                val created = magazineService.createMagazine(user, body)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                val user = call.currentUser(authService)
                val id = call.parseId()
                val body = call.receive<MagazineUpdateRequest>()
                val updated = magazineService.updateMagazine(id, user, body)
                call.respond(updated)
            }

            delete("/{id}") {
                val user = call.currentUser(authService)
                val id = call.parseId()
                magazineService.deleteMagazine(id, user)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/cover") {
                val id = call.parseId()
                val userId = call.userIdOrThrow()
                val userRole = call.userRoleOrThrow()
                var coverPart: PartData.FileItem? = null
                call.receiveMultipart().forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "cover" && coverPart == null) {
                                coverPart = part
                            } else {
                                part.dispose()
                            }
                        }
                        else -> part.dispose()
                    }
                }
                val fileItem = coverPart ?: throw BadRequestException("Missing 'cover' file part")
                val dto = magazineService.uploadCover(id, userId, userRole, fileItem)
                call.respond(dto)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.parseId(): UUID {
    val raw = parameters["id"] ?: throw BadRequestException("Magazine id is required")
    return try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid magazine id: $raw")
    }
}
