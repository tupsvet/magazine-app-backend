package com.magazines.service

import com.magazines.config.JwtConfig
import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.toDto
import com.magazines.data.repository.UserRepository
import com.magazines.domain.exception.ConflictException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.exception.UnauthorizedException

import com.magazines.domain.model.User
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtConfig: JwtConfig,
) {

    suspend fun register(email: String, password: String, displayName: String?): AuthResponse {
        val normalized = email.trim().lowercase()
        if (userRepository.findByEmail(normalized) != null) {
            throw ConflictException("Email already registered: $email")
        }
        val user = userRepository.create(
            email = normalized,
            passwordHash = passwordHasher.hash(password),
            displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
        )
        return user.toAuthResponse()
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val normalized = email.trim().lowercase()
        val user = userRepository.findByEmail(normalized)
            ?: throw UnauthorizedException("Invalid credentials")
        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        return user.toAuthResponse()
    }

    suspend fun getUserById(id: UUID): User =
        userRepository.findById(id)
            ?: throw NotFoundException("User not found: $id")

    private fun User.toAuthResponse(): AuthResponse {
        val token = jwtConfig.makeToken(id, email, role.name)
        return AuthResponse(token = token, user = toDto())
    }
}
