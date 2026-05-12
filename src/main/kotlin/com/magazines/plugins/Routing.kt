package com.magazines.plugins

import com.magazines.routes.authRoutes
import com.magazines.routes.categoryRoutes
import com.magazines.service.AuthService
import com.magazines.service.CategoryService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService by inject<AuthService>()
    val categoryService by inject<CategoryService>()

    routing {
        get("/") {
            call.respondText("Magazines Catalog API v1.0")
        }

        authRoutes(authService)
        categoryRoutes(categoryService, authService)
    }
}
