package com.magazines.plugins

import com.magazines.routes.authRoutes
import com.magazines.routes.categoryRoutes
import com.magazines.routes.magazineRoutes
import com.magazines.service.AuthService
import com.magazines.service.CategoryService
import com.magazines.service.MagazineService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import java.io.File

fun Application.configureRouting() {
    val authService by inject<AuthService>()
    val categoryService by inject<CategoryService>()
    val magazineService by inject<MagazineService>()
    val storagePath by inject<String>(named("storagePath"))

    routing {
        get("/") {
            call.respondText("Magazines Catalog API v1.0")
        }

        val coversDir = File("$storagePath/covers")
        println("=== DEBUG ===")
        println("storagePath = $storagePath")
        println("coversDir absolute = ${coversDir.absolutePath}")
        println("coversDir exists = ${coversDir.exists()}")
        println("files = ${coversDir.list()?.joinToString()}")
        println("=============")
        staticFiles("/files/covers", coversDir)

        staticFiles("/files/covers", File("$storagePath/covers"))
        staticFiles("/files/pdfs", File("$storagePath/pdfs"))

        authRoutes(authService)
        categoryRoutes(categoryService, authService)
        magazineRoutes(magazineService, authService)
    }
}
