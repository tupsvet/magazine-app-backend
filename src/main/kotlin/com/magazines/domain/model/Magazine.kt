package com.magazines.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Magazine(
    val id: UUID,
    val title: String,
    val publisher: String?,
    val yearFounded: Int?,
    val categoryId: Int?,
    val description: String?,
    val coverPath: String?,
    val uploadedBy: UUID?,
    val status: MagazineStatus,
    val rejectionReason: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
