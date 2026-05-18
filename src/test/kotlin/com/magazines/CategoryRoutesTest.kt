package com.magazines

import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.CategoryCreateRequest
import com.magazines.data.dto.CategoryDto
import com.magazines.data.dto.RegisterRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRoutesTest : IntegrationTestBase() {

    @Test
    fun `GET api categories returns non-empty list`() = testApplication {
        val response = client.get("/api/categories")

        assertEquals(HttpStatusCode.OK, response.status)
        val categories: List<CategoryDto> = response.jsonBody()
        assertTrue(categories.isNotEmpty())
    }

    @Test
    fun `POST api categories without admin returns 403`() = testApplication {
        val auth: AuthResponse = client.postJson(
            "/api/auth/register",
            RegisterRequest(
                email = "user-${UUID.randomUUID()}@test.com",
                password = "password123",
                displayName = "Test User",
            ),
        ).jsonBody()

        val response = client.postJson(
            "/api/categories",
            CategoryCreateRequest(name = "New Category", description = "Test"),
        ) {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
