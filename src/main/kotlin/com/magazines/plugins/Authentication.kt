package com.magazines.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication

fun Application.configureAuthentication() {
    install(Authentication) {
        // JWT / Firebase providers will be configured here
    }
}
