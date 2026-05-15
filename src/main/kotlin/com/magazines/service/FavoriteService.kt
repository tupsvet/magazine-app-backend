package com.magazines.service

import com.magazines.data.dto.MagazineDto
import com.magazines.data.repository.CategoryRepository
import com.magazines.data.repository.FavoriteRepository
import com.magazines.data.repository.MagazineRepository
import com.magazines.data.repository.MagazineStats
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.Magazine
import com.magazines.domain.model.MagazineStatus
import java.util.UUID

class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val magazineRepository: MagazineRepository,
    private val categoryRepository: CategoryRepository,
    private val baseUrl: String,
) {

    fun getUserFavorites(userId: UUID): List<MagazineDto> {
        val magazines = favoriteRepository.findByUser(userId)
        return enrichMany(magazines)
    }

    fun addToFavorites(userId: UUID, magazineId: UUID): Boolean {
        requireApprovedMagazine(magazineId)
        return favoriteRepository.add(userId, magazineId)
    }

    fun removeFromFavorites(userId: UUID, magazineId: UUID) {
        magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")

        if (!favoriteRepository.remove(userId, magazineId)) {
            throw NotFoundException("Favorite not found for magazine: $magazineId")
        }
    }

    fun getMagazineDto(magazineId: UUID): MagazineDto {
        val magazine = requireApprovedMagazine(magazineId)
        return enrichOne(magazine)
    }

    private fun requireApprovedMagazine(magazineId: UUID): Magazine {
        val magazine = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")
        if (magazine.status != MagazineStatus.APPROVED) {
            throw NotFoundException("Magazine not found: $magazineId")
        }
        return magazine
    }

    private fun enrichOne(magazine: Magazine): MagazineDto {
        val stats = magazineRepository.getStats(magazine.id)
        val categoryName = magazine.categoryId?.let { categoryRepository.findById(it)?.name }
        return magazine.toDto(categoryName, stats, baseUrl)
    }

    private fun enrichMany(items: List<Magazine>): List<MagazineDto> {
        if (items.isEmpty()) return emptyList()
        val statsById = magazineRepository.getStatsForMany(items.map { it.id })
        val categoryIds = items.mapNotNull { it.categoryId }.toSet()
        val categoryNames = categoryIds.associateWith { categoryRepository.findById(it)?.name }
        return items.map { mag ->
            mag.toDto(
                categoryName = mag.categoryId?.let { categoryNames[it] },
                stats = statsById[mag.id] ?: MagazineStats(0.0, 0, 0),
                baseUrl = baseUrl,
            )
        }
    }
}

private fun Magazine.toDto(categoryName: String?, stats: MagazineStats, baseUrl: String): MagazineDto =
    MagazineDto(
        id = id.toString(),
        title = title,
        publisher = publisher,
        yearFounded = yearFounded,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        coverUrl = coverPath?.let { "$baseUrl/files/covers/$it" },
        uploadedBy = uploadedBy?.toString(),
        status = status.name,
        averageRating = stats.averageRating,
        reviewsCount = stats.reviewsCount,
        issuesCount = stats.issuesCount,
        createdAt = createdAt.toString(),
    )
