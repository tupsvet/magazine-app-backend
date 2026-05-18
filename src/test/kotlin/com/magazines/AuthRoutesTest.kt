package com.magazines

import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.LoginRequest
import com.magazines.data.dto.RegisterRequest
import com.magazines.data.dto.UserDto
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest : IntegrationTestBase() {

    @Test
    fun `successful registration`() = testApplication {
        val email = "user-${UUID.randomUUID()}@test.com"

        val response = client.postJson(
            "/api/auth/register",
            RegisterRequest(
                email = email,
                password = "password123",
                displayName = "Test User",
            ),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val auth: AuthResponse = response.jsonBody()
        assertTrue(auth.token.isNotBlank())
        assertEquals(email.trim().lowercase(), auth.user.email)
        assertEquals("USER", auth.user.role)
    }

    @Test
    fun `duplicate registration with same email returns 409 Conflict`() = testApplication {
        val email = "user-${UUID.randomUUID()}@test.com"
        val request = RegisterRequest(
            email = email,
            password = "password123",
            displayName = "Test User",
        )

        client.postJson("/api/auth/register", request)

        val response = client.postJson("/api/auth/register", request)

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `login with correct password returns 200 and token`() = testApplication {
        val email = "user-${UUID.randomUUID()}@test.com"
        val password = "password123"

        client.postJson("/api/auth/register", RegisterRequest(email = email, password = password))

        val response = client.postJson("/api/auth/login", LoginRequest(email = email, password = password))

        assertEquals(HttpStatusCode.OK, response.status)
        val auth: AuthResponse = response.jsonBody()
        assertTrue(auth.token.isNotBlank())
        assertEquals(email.trim().lowercase(), auth.user.email)
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        val email = "user-${UUID.randomUUID()}@test.com"

        client.postJson("/api/auth/register", RegisterRequest(email = email, password = "password123"))

        val response = client.postJson(
            "/api/auth/login",
            LoginRequest(email = email, password = "wrong-password"),
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET api auth me with valid token returns 200`() = testApplication {
        val email = "user-${UUID.randomUUID()}@test.com"
        val auth: AuthResponse = client.postJson(
            "/api/auth/register",
            RegisterRequest(email = email, password = "password123", displayName = "Me User"),
        ).jsonBody()

        val response = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val user: UserDto = response.jsonBody()
        assertEquals(auth.user.id, user.id)
        assertEquals(email.trim().lowercase(), user.email)
        assertEquals("Me User", user.displayName)
    }

    @Test
    fun `GET api auth me without token returns 401`() = testApplication {
        val response = client.get("/api/auth/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
