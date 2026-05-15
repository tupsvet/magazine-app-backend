package com.magazines.service

import com.magazines.data.dto.ReviewCreateRequest
import com.magazines.data.dto.ReviewDto
import com.magazines.data.dto.ReviewUpdateRequest
import com.magazines.data.repository.MagazineRepository
import com.magazines.data.repository.ReviewRepository
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.ConflictException
import com.magazines.domain.exception.ForbiddenException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.MagazineStatus
import com.magazines.domain.model.Review
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.util.UUID

class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val magazineRepository: MagazineRepository,
) {

    fun createReview(magazineId: UUID, userId: UUID, request: ReviewCreateRequest): ReviewDto {
        requireApprovedMagazine(magazineId)
        validateRating(request.rating)

        if (reviewRepository.findByUserAndMagazine(userId, magazineId) != null) {
            throw ConflictException("Already reviewed, use PUT to update")
        }

        val review = reviewRepository.create(
            magazineId = magazineId,
            userId = userId,
            rating = request.rating,
            comment = request.comment,
        )
        return review.toDto()
    }

    fun updateReview(reviewId: UUID, userId: UUID, request: ReviewUpdateRequest): ReviewDto {
        val existing = reviewRepository.findById(reviewId)
            ?: throw NotFoundException("Review not found: $reviewId")

        if (existing.userId != userId) {
            throw ForbiddenException("Only the review author can update this review")
        }

        request.rating?.let { validateRating(it) }

        if (request.rating == null && request.comment == null) {
            throw BadRequestException("At least one of rating or comment must be provided")
        }

        val updated = reviewRepository.update(
            id = reviewId,
            rating = request.rating,
            comment = request.comment,
        ) ?: throw NotFoundException("Review not found: $reviewId")

        return updated.toDto()
    }

    fun deleteReview(reviewId: UUID, userId: UUID, isAdmin: Boolean): Boolean {
        val existing = reviewRepository.findById(reviewId)
            ?: throw NotFoundException("Review not found: $reviewId")

        if (!isAdmin && existing.userId != userId) {
            throw ForbiddenException("Only the review author or admin can delete this review")
        }

        if (!reviewRepository.delete(reviewId)) {
            throw NotFoundException("Review not found: $reviewId")
        }
        return true
    }

    fun getReviewsByMagazine(magazineId: UUID): List<ReviewDto> {
        requireApprovedMagazine(magazineId)
        return reviewRepository.findByMagazine(magazineId).map { it.toDto() }
    }

    fun getAverageRating(magazineId: UUID): Double? {
        requireApprovedMagazine(magazineId)
        return reviewRepository.averageRating(magazineId)?.let { roundTo1(it) }
    }

    private fun requireApprovedMagazine(magazineId: UUID) {
        val magazine = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")
        if (magazine.status != MagazineStatus.APPROVED) {
            throw NotFoundException("Magazine not found: $magazineId")
        }
    }

    private fun validateRating(rating: Int) {
        if (rating !in 1..5) {
            throw BadRequestException("Rating must be between 1 and 5")
        }
    }

    private fun roundTo1(value: Double): Double =
        BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble()
}

private fun Review.toDto(): ReviewDto =
    ReviewDto(
        id = id.toString(),
        magazineId = magazineId.toString(),
        userId = userId.toString(),
        userName = userName ?: userId.toString(),
        rating = rating,
        comment = comment,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant(),
    )
