package com.magazines.data.dto

import com.magazines.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String?,
    val role: String,
    val createdAt: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
)

fun User.toDto() = UserDto(
    id = id.toString(),
    email = email,
    displayName = displayName,
    role = role.name,
    createdAt = createdAt.toString(),
)
