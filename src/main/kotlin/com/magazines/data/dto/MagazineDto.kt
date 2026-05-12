package com.magazines.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MagazineDto(
    val id: String,
    val title: String,
    val publisher: String?,
    val yearFounded: Int?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val coverUrl: String?,
    val uploadedBy: String?,
    val status: String,
    val averageRating: Double,
    val reviewsCount: Int,
    val issuesCount: Int,
    val createdAt: String,
)

@Serializable
data class MagazineCreateRequest(
    val title: String,
    val publisher: String? = null,
    val yearFounded: Int? = null,
    val categoryId: Int? = null,
    val description: String? = null,
)

@Serializable
data class MagazineUpdateRequest(
    val title: String? = null,
    val publisher: String? = null,
    val yearFounded: Int? = null,
    val categoryId: Int? = null,
    val description: String? = null,
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
)
