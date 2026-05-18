package com.magazines

import com.magazines.data.dto.AuthResponse
import com.magazines.data.dto.MagazineCreateRequest
import com.magazines.data.dto.MagazineDto
import com.magazines.data.dto.RegisterRequest
import com.magazines.data.dto.ReviewCreateRequest
import com.magazines.data.dto.ReviewDto
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class ReviewRoutesTest : IntegrationTestBase() {

    @Test
    fun `second review from same user on same magazine returns 409 Conflict`() = testApplication {
        val magazine = createApprovedMagazine()
        val auth = registerUser()

        postReview(auth.token, magazine.id, rating = 4, comment = "First review")
            .let { assertEquals(HttpStatusCode.Created, it.status) }

        val second = postReview(auth.token, magazine.id, rating = 5, comment = "Duplicate")

        assertEquals(HttpStatusCode.Conflict, second.status)
    }

    @Test
    fun `rating outside valid range returns 400 Bad Request`() = testApplication {
        val magazine = createApprovedMagazine()
        val auth = registerUser()

        val response = postReview(auth.token, magazine.id, rating = 0)

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `successful review creation updates magazine average rating`() = testApplication {
        val magazine = createApprovedMagazine()
        val auth = registerUser()

        val review: ReviewDto = postReview(auth.token, magazine.id, rating = 4, comment = "Great read")
            .let {
                assertEquals(HttpStatusCode.Created, it.status)
                it.jsonBody()
            }

        assertEquals(4, review.rating)
        assertEquals(magazine.id, review.magazineId)

        val updated: MagazineDto = client.get("/api/magazines/${magazine.id}").jsonBody()

        assertEquals(4.0, updated.averageRating)
        assertEquals(1, updated.reviewsCount)
    }

    private suspend fun ApplicationTestBuilder.createApprovedMagazine(): MagazineDto {
        val email = "admin-${UUID.randomUUID()}@test.com"
        val auth = registerUser(email)
        promoteToAdmin(email)
        return createMagazine(auth.token, "Review Test Magazine ${UUID.randomUUID()}")
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

    private suspend fun ApplicationTestBuilder.postReview(
        token: String,
        magazineId: String,
        rating: Int,
        comment: String? = null,
    ): HttpResponse = client.postJson(
        "/api/magazines/$magazineId/reviews",
        ReviewCreateRequest(rating = rating, comment = comment),
    ) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
}
