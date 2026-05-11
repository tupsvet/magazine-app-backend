package com.magazines.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String?,
    val role: UserRole,
    val createdAt: LocalDateTime,
)
