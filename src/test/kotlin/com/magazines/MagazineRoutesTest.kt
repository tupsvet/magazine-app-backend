package com.magazines

import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.MagazineCreateRequest
import com.magazines.data.dto.MagazineDto
import com.magazines.data.dto.PagedResponse
import com.magazines.data.dto.RegisterRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MagazineRoutesTest : IntegrationTestBase() {

    @Test
    fun `regular user creates magazine with PENDING status`() = testApplication {
        val auth = registerUser()

        val magazine = createMagazine(auth.token, "User Magazine ${UUID.randomUUID()}")

        assertEquals("PENDING", magazine.status)
    }

    @Test
    fun `admin creates magazine with APPROVED status`() = testApplication {
        val email = "admin-${UUID.randomUUID()}@test.com"
        val auth = registerUser(email)
        promoteToAdmin(email)

        val magazine = createMagazine(auth.token, "Admin Magazine ${UUID.randomUUID()}")

        assertEquals("APPROVED", magazine.status)
    }

    @Test
    fun `public GET api magazines does not show PENDING magazines`() = testApplication {
        val auth = registerUser()
        val pending = createMagazine(auth.token, "Pending Magazine ${UUID.randomUUID()}")
        assertEquals("PENDING", pending.status)

        val publicList: PagedResponse<MagazineDto> = client.get("/api/magazines").jsonBody()

        assertFalse(publicList.items.any { it.id == pending.id })
    }

    @Test
    fun `owner sees own PENDING magazines via GET api magazines mine`() = testApplication {
        val auth = registerUser()
        val pending = createMagazine(auth.token, "My Pending Magazine ${UUID.randomUUID()}")
        assertEquals("PENDING", pending.status)

        val mine: List<MagazineDto> = client.get("/api/magazines/mine") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }.jsonBody()

        assertTrue(mine.any { it.id == pending.id && it.status == "PENDING" })
    }

    private suspend fun ApplicationTestBuilder.registerUser(
        email: String = "user-${UUID.randomUUID()}@test.com",
    ): AuthResponse = client.postJson(
        "/api/auth/register",
        RegisterRequest(
            email = email,
            password = "password123",
            displayName = "Test User",
        ),
    ).jsonBody()

    private suspend fun ApplicationTestBuilder.createMagazine(
        token: String,
        title: String,
    ): MagazineDto {
        val response = client.postJson(
            "/api/magazines",
            MagazineCreateRequest(
                title = title,
                categoryId = 1,
            ),
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.jsonBody()
    }
}
