package com.magazines.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Review(
    val id: UUID,
    val magazineId: UUID,
    val userId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
)
