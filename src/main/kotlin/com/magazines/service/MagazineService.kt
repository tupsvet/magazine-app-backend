package com.magazines.service

import com.magazines.data.dto.MagazineCreateRequest
import com.magazines.data.dto.MagazineDto
import com.magazines.data.dto.MagazineUpdateRequest
import com.magazines.data.dto.PagedResponse
import com.magazines.data.repository.CategoryRepository
import com.magazines.data.repository.MagazineRepository
import com.magazines.data.repository.MagazineStats
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.ForbiddenException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.Magazine
import com.magazines.domain.model.MagazineStatus
import com.magazines.domain.model.User
import com.magazines.domain.model.UserRole
import io.ktor.http.content.PartData
import java.util.UUID

class MagazineService(
    private val magazineRepository: MagazineRepository,
    private val categoryRepository: CategoryRepository,
    private val fileStorageService: FileStorageService,
    private val baseUrl: String,
) {

    fun listPublic(category: Int?, search: String?, page: Int, pageSize: Int): PagedResponse<MagazineDto> {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, 100)

        val result = magazineRepository.findApproved(category, search, safePage, safeSize)
        val items = enrichMany(result.items)

        val totalPages = if (safeSize == 0) 0
        else ((result.totalItems + safeSize - 1) / safeSize).toInt()

        return PagedResponse(
            items = items,
            page = safePage,
            pageSize = safeSize,
            totalItems = result.totalItems,
            totalPages = totalPages,
        )
    }

    fun getPublicMagazine(id: UUID, currentUser: User?): MagazineDto {
        val magazine = magazineRepository.findById(id)
            ?: throw NotFoundException("Magazine not found: $id")

        if (magazine.status != MagazineStatus.APPROVED) {
            val isOwner = currentUser != null && magazine.uploadedBy == currentUser.id
            val isAdmin = currentUser?.role == UserRole.ADMIN
            if (!isOwner && !isAdmin) {
                throw NotFoundException("Magazine not found: $id")
            }
        }
        return enrichOne(magazine)
    }

    fun listMine(userId: UUID): List<MagazineDto> {
        val magazines = magazineRepository.findByUploader(userId)
        return enrichMany(magazines)
    }

    fun createMagazine(user: User, req: MagazineCreateRequest): MagazineDto {
        val title = req.title.trim()
        if (title.isEmpty()) {
            throw BadRequestException("Title must not be empty")
        }
        req.categoryId?.let {
            categoryRepository.findById(it) ?: throw BadRequestException("Category not found: $it")
        }
        req.yearFounded?.let {
            if (it < 1500 || it > 9999) throw BadRequestException("Invalid yearFounded: $it")
        }

        val status = if (user.role == UserRole.ADMIN) MagazineStatus.APPROVED else MagazineStatus.PENDING

        val magazine = magazineRepository.create(
            title = title,
            publisher = req.publisher?.trim()?.takeIf { it.isNotEmpty() },
            yearFounded = req.yearFounded,
            categoryId = req.categoryId,
            description = req.description?.trim()?.takeIf { it.isNotEmpty() },
            uploadedBy = user.id,
            status = status,
        )
        return enrichOne(magazine)
    }

    fun updateMagazine(id: UUID, user: User, req: MagazineUpdateRequest): MagazineDto {
        val existing = magazineRepository.findById(id)
            ?: throw NotFoundException("Magazine not found: $id")
        requireOwnerOrAdmin(existing, user)

        val title = req.title?.trim()?.also {
            if (it.isEmpty()) throw BadRequestException("Title must not be empty")
        }
        req.categoryId?.let {
            categoryRepository.findById(it) ?: throw BadRequestException("Category not found: $it")
        }
        req.yearFounded?.let {
            if (it < 1500 || it > 9999) throw BadRequestException("Invalid yearFounded: $it")
        }

        val updated = magazineRepository.update(
            id = id,
            title = title,
            publisher = req.publisher?.trim(),
            yearFounded = req.yearFounded,
            categoryId = req.categoryId,
            description = req.description?.trim(),
        ) ?: throw NotFoundException("Magazine not found: $id")

        return enrichOne(updated)
    }

    fun deleteMagazine(id: UUID, user: User) {
        val existing = magazineRepository.findById(id)
            ?: throw NotFoundException("Magazine not found: $id")
        requireOwnerOrAdmin(existing, user)
        magazineRepository.delete(id)
    }

    fun uploadCover(magazineId: UUID, userId: UUID, userRole: UserRole, fileItem: PartData.FileItem): MagazineDto {
        val magazine = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")

        val isAdmin = userRole == UserRole.ADMIN
        val isOwner = magazine.uploadedBy == userId
        if (!isAdmin && !isOwner) {
            throw ForbiddenException("Only owner or admin can upload cover")
        }

        magazine.coverPath?.let { oldPath ->
            fileStorageService.deleteFile(oldPath, "covers")
        }

        val newPath = fileStorageService.saveCover(magazineId, fileItem)
        if (!magazineRepository.updateCoverPath(magazineId, newPath)) {
            throw NotFoundException("Magazine not found: $magazineId")
        }

        val updated = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")
        return enrichOne(updated)
    }

    fun calculateAverageRating(magazineId: UUID): Double =
        magazineRepository.getStats(magazineId).averageRating

    private fun requireOwnerOrAdmin(magazine: Magazine, user: User) {
        if (user.role == UserRole.ADMIN) return
        if (magazine.uploadedBy == user.id) return
        throw ForbiddenException("Not allowed to modify this magazine")
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
