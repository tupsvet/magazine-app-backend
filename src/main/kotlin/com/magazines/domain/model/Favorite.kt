package com.magazines.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Favorite(
    val userId: UUID,
    val magazineId: UUID,
    val addedAt: LocalDateTime,
)
