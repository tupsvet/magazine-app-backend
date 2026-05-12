package com.magazines.routes

import com.magazines.data.dto.CategoryCreateRequest
import com.magazines.data.dto.CategoryUpdateRequest
import com.magazines.data.dto.toDto
import com.magazines.domain.exception.BadRequestException
import com.magazines.service.AuthService
import com.magazines.service.CategoryService
import com.magazines.util.currentUser
import com.magazines.util.requireAdmin
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

fun Route.categoryRoutes(
    categoryService: CategoryService,
    authService: AuthService,
) {
    route("/api/categories") {
        get {
            val categories = categoryService.list().map { it.toDto() }
            call.respond(categories)
        }

        authenticate("auth-jwt") {
            post {
                val user = call.currentUser(authService)
                requireAdmin(user)
                val body = call.receive<CategoryCreateRequest>()
                val created = categoryService.create(body.name, body.description)
                call.respond(HttpStatusCode.Created, created.toDto())
            }

            put("/{id}") {
                val user = call.currentUser(authService)
                requireAdmin(user)
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid category id")
                val body = call.receive<CategoryUpdateRequest>()
                val updated = categoryService.update(id, body.name, body.description)
                call.respond(updated.toDto())
            }

            delete("/{id}") {
                val user = call.currentUser(authService)
                requireAdmin(user)
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw BadRequestException("Invalid category id")
                categoryService.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
