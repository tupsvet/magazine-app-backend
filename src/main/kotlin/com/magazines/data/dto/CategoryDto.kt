package com.magazines.data.dto

import com.magazines.domain.model.Category
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val description: String?,
)

@Serializable
data class CategoryCreateRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class CategoryUpdateRequest(
    val name: String? = null,
    val description: String? = null,
)

fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    description = description,
)
