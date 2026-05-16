package com.magazines.service

import com.magazines.data.repository.MagazineRepository
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.Magazine
import com.magazines.domain.model.MagazineStatus
import java.util.UUID

class ModerationService(
    private val magazineRepository: MagazineRepository,
) {

    fun listPending(): List<Magazine> = magazineRepository.findPending()

    fun approve(magazineId: UUID): Magazine {
        val magazine = requireExisting(magazineId)
        requirePending(magazine)
        return magazineRepository.approve(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")
    }

    fun reject(magazineId: UUID, reason: String?): Magazine {
        val magazine = requireExisting(magazineId)
        requirePending(magazine)
        return magazineRepository.reject(magazineId, reason)
            ?: throw NotFoundException("Magazine not found: $magazineId")
    }

    private fun requireExisting(magazineId: UUID): Magazine =
        magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")

    private fun requirePending(magazine: Magazine) {
        if (magazine.status != MagazineStatus.PENDING) {
            throw BadRequestException(
                "Magazine must be in PENDING status to moderate, current: ${magazine.status}",
            )
        }
    }
}
