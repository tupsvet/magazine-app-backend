package com.magazines.data.repository

import com.magazines.db.tables.IssuesTable
import com.magazines.domain.model.Issue
import java.time.LocalDate
import java.util.UUID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

interface IssueRepository {
    fun findByMagazine(magazineId: UUID): List<Issue>

    fun findById(id: UUID): Issue?

    fun create(
        magazineId: UUID,
        issueNumber: String,
        publicationDate: String?,
        pdfPath: String?,
        pagesCount: Int?,
    ): Issue

    fun delete(id: UUID): Boolean

    fun countByMagazine(magazineId: UUID): Int

    fun updatePdfPath(id: UUID, pdfPath: String): Boolean
}

class IssueRepositoryImpl : IssueRepository {

    override fun findByMagazine(magazineId: UUID): List<Issue> = transaction {
        IssuesTable
            .select { IssuesTable.magazineId eq magazineId }
            .orderBy(IssuesTable.createdAt to SortOrder.DESC)
            .map { it.toIssue() }
    }

    override fun findById(id: UUID): Issue? = transaction {
        IssuesTable
            .select { IssuesTable.id eq id }
            .singleOrNull()
            ?.toIssue()
    }

    override fun create(
        magazineId: UUID,
        issueNumber: String,
        publicationDate: String?,
        pdfPath: String?,
        pagesCount: Int?,
    ): Issue = transaction {
        val parsedDate = publicationDate
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { LocalDate.parse(it) }

        val insertedId = IssuesTable.insert {
            it[IssuesTable.magazineId] = magazineId
            it[IssuesTable.issueNumber] = issueNumber
            it[IssuesTable.publicationDate] = parsedDate
            it[IssuesTable.pdfPath] = pdfPath?.trim()?.takeIf { p -> p.isNotEmpty() }.orEmpty()
            it[IssuesTable.pagesCount] = pagesCount
        } get IssuesTable.id

        IssuesTable
            .select { IssuesTable.id eq insertedId }
            .single()
            .toIssue()
    }

    override fun delete(id: UUID): Boolean = transaction {
        IssuesTable.deleteWhere { Op.build { IssuesTable.id eq id } } > 0
    }

    override fun countByMagazine(magazineId: UUID): Int = transaction {
        IssuesTable
            .select { IssuesTable.magazineId eq magazineId }
            .count()
            .toInt()
    }

    override fun updatePdfPath(id: UUID, pdfPath: String): Boolean = transaction {
        IssuesTable.update({ IssuesTable.id eq id }) {
            it[IssuesTable.pdfPath] = pdfPath
        } > 0
    }

    private fun ResultRow.toIssue() = Issue(
        id = this[IssuesTable.id].value,
        magazineId = this[IssuesTable.magazineId].value,
        issueNumber = this[IssuesTable.issueNumber],
        publicationDate = this[IssuesTable.publicationDate],
        pdfPath = this[IssuesTable.pdfPath],
        pagesCount = this[IssuesTable.pagesCount],
        createdAt = this[IssuesTable.createdAt],
    )
}
