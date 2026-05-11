package com.magazines.util

import com.magazines.domain.model.User
import com.magazines.domain.model.UserRole
import com.magazines.service.AuthService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

private fun ApplicationCall.principalOrThrow(): JWTPrincipal =
    principal<JWTPrincipal>()
        ?: error("JWTPrincipal missing — route is not wrapped in authenticate(\"auth-jwt\").")

fun ApplicationCall.userIdOrThrow(): UUID {
    val sub = principalOrThrow().payload.subject
        ?: error("JWT subject claim missing.")
    return UUID.fromString(sub)
}

fun ApplicationCall.userRoleOrThrow(): UserRole {
    val role = principalOrThrow().payload.getClaim("role").asString()
        ?: error("JWT role claim missing.")
    return UserRole.valueOf(role)
}

suspend fun ApplicationCall.currentUser(authService: AuthService): User =
    authService.getUserById(userIdOrThrow())
