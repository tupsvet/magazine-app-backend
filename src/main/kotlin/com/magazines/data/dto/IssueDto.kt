package com.magazines.data.dto

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class IssueDto(
    val id: String,
    val magazineId: String,
    val issueNumber: String,
    val publicationDate: String?,
    val pdfUrl: String?,
    val pagesCount: Int?,
    @Serializable(with = IsoInstantSerializer::class)
    val createdAt: Instant,
)

@Serializable
data class IssueCreateRequest(
    val issueNumber: String,
    val publicationDate: String? = null,
    val pagesCount: Int? = null,
)
