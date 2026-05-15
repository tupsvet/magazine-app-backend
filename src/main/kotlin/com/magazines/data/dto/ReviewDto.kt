package com.magazines.data.dto

import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String,
    val magazineId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String?,
    @Serializable(with = IsoInstantSerializer::class)
    val createdAt: Instant,
)

@Serializable
data class ReviewCreateRequest(
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class ReviewUpdateRequest(
    val rating: Int? = null,
    val comment: String? = null,
)
