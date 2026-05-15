package com.magazines.routes

import com.magazines.data.dto.IssueCreateRequest
import com.magazines.domain.exception.BadRequestException
import com.magazines.service.AuthService
import com.magazines.service.IssueService
import com.magazines.util.extractUserId
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.issueRoutes(
    issueService: IssueService,
    authService: AuthService,
) {
    route("/api/magazines") {
        get("{id}/issues") {
            val magazineId = call.parseMagazineId()
            call.respond(issueService.getIssuesByMagazine(magazineId))
        }

        authenticate("auth-jwt") {
            post("{id}/issues") {
                val magazineId = call.parseMagazineId()
                val user = authService.getUserById(call.extractUserId())

                var issueNumber: String? = null
                var publicationDate: String? = null
                var pagesCount: Int? = null
                var pdfPart: PartData.FileItem? = null

                call.receiveMultipart().forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "issueNumber" -> issueNumber = part.value
                                "publicationDate" -> {
                                    val v = part.value.trim()
                                    publicationDate = v.takeIf { it.isNotEmpty() }
                                }
                                "pagesCount" -> {
                                    val v = part.value.trim()
                                    pagesCount = when {
                                        v.isEmpty() -> null
                                        else -> v.toIntOrNull()
                                            ?: throw BadRequestException("Invalid pagesCount: ${part.value}")
                                    }
                                }
                            }
                        }
                        is PartData.FileItem -> {
                            if (part.name == "pdf" && pdfPart == null) {
                                pdfPart = part
                            } else {
                                part.dispose()
                            }
                        }
                        else -> part.dispose()
                    }
                }

                val number = issueNumber?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw BadRequestException("Missing or empty issueNumber")
                val fileItem = pdfPart ?: throw BadRequestException("Missing 'pdf' file part")

                val request = IssueCreateRequest(
                    issueNumber = number,
                    publicationDate = publicationDate,
                    pagesCount = pagesCount,
                )
                val dto = issueService.createIssueWithPdf(magazineId, request, fileItem, user)
                call.respond(HttpStatusCode.Created, dto)
            }
        }
    }

    route("/api/issues") {
        authenticate("auth-jwt") {
            delete("{id}") {
                val issueId = call.parseIssueId()
                val user = authService.getUserById(call.extractUserId())
                issueService.deleteIssue(issueId, user)
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

private fun io.ktor.server.application.ApplicationCall.parseIssueId(): UUID {
    val raw = parameters["id"] ?: throw BadRequestException("Issue id is required")
    return try {
        UUID.fromString(raw)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid issue id: $raw")
    }
}
