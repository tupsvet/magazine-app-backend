package com.magazines.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Issue(
    val id: UUID,
    val magazineId: UUID,
    val issueNumber: String,
    val publicationDate: LocalDate?,
    val pdfPath: String,
    val pagesCount: Int?,
    val createdAt: LocalDateTime,
)
