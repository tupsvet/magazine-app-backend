package com.magazines.service

import com.magazines.data.dto.IssueCreateRequest
import com.magazines.data.dto.IssueDto
import com.magazines.data.repository.IssueRepository
import com.magazines.data.repository.MagazineRepository
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.ForbiddenException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.Issue
import com.magazines.domain.model.MagazineStatus
import com.magazines.domain.model.User
import com.magazines.domain.model.UserRole
import io.ktor.http.content.PartData
import java.time.ZoneId
import java.util.UUID

class IssueService(
    private val issueRepository: IssueRepository,
    private val magazineRepository: MagazineRepository,
    private val magazineService: MagazineService,
    private val fileStorageService: FileStorageService,
    private val baseUrl: String,
) {

    fun createIssue(
        magazineId: UUID,
        request: IssueCreateRequest,
        pdfPath: String?,
        user: User,
    ): Issue {
        val magazine = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")

        ensureCanManageIssues(magazineId, user)

        val issueNumber = request.issueNumber.trim()
        if (issueNumber.isEmpty()) {
            throw BadRequestException("issueNumber must not be empty")
        }

        return issueRepository.create(
            magazineId = magazine.id,
            issueNumber = issueNumber,
            publicationDate = request.publicationDate,
            pdfPath = pdfPath,
            pagesCount = request.pagesCount,
        )
    }

    fun getIssuesByMagazine(magazineId: UUID): List<IssueDto> {
        val magazine = magazineRepository.findById(magazineId)
            ?: throw NotFoundException("Magazine not found: $magazineId")

        if (magazine.status != MagazineStatus.APPROVED) {
            throw NotFoundException("Magazine not found: $magazineId")
        }

        return issueRepository.findByMagazine(magazineId).map { it.toIssueDto(baseUrl) }
    }

    fun createIssueWithPdf(
        magazineId: UUID,
        request: IssueCreateRequest,
        pdfPart: PartData.FileItem,
        user: User,
    ): IssueDto {
        val issue = createIssue(magazineId, request, pdfPath = "", user)
        try {
            val relativePath = fileStorageService.savePdf(issue.id, pdfPart)
            if (!issueRepository.updatePdfPath(issue.id, relativePath)) {
                throw NotFoundException("Issue not found: ${issue.id}")
            }
            return issueRepository.findById(issue.id)?.toIssueDto(baseUrl)
                ?: throw NotFoundException("Issue not found: ${issue.id}")
        } catch (e: Throwable) {
            fileStorageService.deleteFile("${issue.id}.pdf", "pdfs")
            issueRepository.delete(issue.id)
            throw e
        }
    }

    fun deleteIssue(issueId: UUID, user: User): Boolean {
        val issue = issueRepository.findById(issueId)
            ?: throw NotFoundException("Issue not found: $issueId")

        ensureCanManageIssues(issue.magazineId, user)

        val pdfPath = issue.pdfPath
        if (!issueRepository.delete(issueId)) {
            throw NotFoundException("Issue not found: $issueId")
        }
        if (pdfPath.isNotEmpty()) {
            fileStorageService.deleteFile(pdfPath, "pdfs")
        }
        return true
    }

    private fun ensureCanManageIssues(magazineId: UUID, user: User) {
        if (user.role == UserRole.ADMIN) return
        if (magazineService.isOwner(magazineId, user.id)) return
        throw ForbiddenException("Only magazine owner or admin can manage issues")
    }
}

private fun Issue.toIssueDto(baseUrl: String): IssueDto =
    IssueDto(
        id = id.toString(),
        magazineId = magazineId.toString(),
        issueNumber = issueNumber,
        publicationDate = publicationDate?.toString(),
        pdfUrl = if (pdfPath.isNotEmpty()) "${baseUrl}/files/pdfs/${pdfPath}" else null,
        pagesCount = pagesCount,
        createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant(),
    )
