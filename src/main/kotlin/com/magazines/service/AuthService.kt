package com.magazines.service

import com.magazines.config.JwtConfig
import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.toDto
import com.magazines.data.repository.UserRepository
import com.magazines.domain.exception.EmailAlreadyTakenException
import com.magazines.domain.exception.InvalidCredentialsException
import com.magazines.domain.exception.UserNotFoundException
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
            throw EmailAlreadyTakenException(normalized)
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
            ?: throw InvalidCredentialsException()
        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return user.toAuthResponse()
    }

    suspend fun getUserById(id: UUID): User =
        userRepository.findById(id)
            ?: throw UserNotFoundException(id.toString())

    private fun User.toAuthResponse(): AuthResponse {
        val token = jwtConfig.makeToken(id, email, role.name)
        return AuthResponse(token = token, user = toDto())
    }
}
