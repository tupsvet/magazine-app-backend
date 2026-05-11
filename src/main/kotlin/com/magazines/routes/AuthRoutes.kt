package com.magazines.routes

import com.magazines.data.dto.LoginRequest
import com.magazines.data.dto.RegisterRequest
import com.magazines.data.dto.toDto
import com.magazines.service.AuthService
import com.magazines.util.currentUser
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val body = call.receive<RegisterRequest>()
            val response = authService.register(
                email = body.email,
                password = body.password,
                displayName = body.displayName,
            )
            call.respond(response)
        }

        post("/login") {
            val body = call.receive<LoginRequest>()
            val response = authService.login(body.email, body.password)
            call.respond(response)
        }

        authenticate("auth-jwt") {
            get("/me") {
                val user = call.currentUser(authService)
                call.respond(user.toDto())
            }
        }
    }
}
