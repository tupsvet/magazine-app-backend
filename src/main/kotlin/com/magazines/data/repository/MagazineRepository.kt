package com.magazines.data.repository

import com.magazines.db.tables.IssuesTable
import com.magazines.db.tables.MagazinesTable
import com.magazines.db.tables.ReviewsTable
import com.magazines.domain.model.Magazine
import com.magazines.domain.model.MagazineStatus
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.stringParam
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class PagedResult<T>(
    val items: List<T>,
    val totalItems: Long,
)

data class MagazineStats(
    val averageRating: Double,
    val reviewsCount: Int,
    val issuesCount: Int,
)

interface MagazineRepository {
    fun findApproved(category: Int?, search: String?, page: Int, pageSize: Int): PagedResult<Magazine>
    fun findById(id: UUID): Magazine?
    fun findByUploader(userId: UUID): List<Magazine>
    fun findPending(): List<Magazine>
    fun create(
        title: String,
        publisher: String?,
        yearFounded: Int?,
        categoryId: Int?,
        description: String?,
        uploadedBy: UUID?,
        status: MagazineStatus,
    ): Magazine
    fun update(
        id: UUID,
        title: String?,
        publisher: String?,
        yearFounded: Int?,
        categoryId: Int?,
        description: String?,
    ): Magazine?
    fun updateStatus(id: UUID, status: MagazineStatus): Magazine?
    fun updateCoverPath(id: UUID, path: String?): Boolean
    fun delete(id: UUID): Boolean

    fun getStats(id: UUID): MagazineStats
    fun getStatsForMany(ids: List<UUID>): Map<UUID, MagazineStats>
}

class MagazineRepositoryImpl : MagazineRepository {

    override fun findApproved(
        category: Int?,
        search: String?,
        page: Int,
        pageSize: Int,
    ): PagedResult<Magazine> = transaction {
        val condition: Op<Boolean> = SqlExpressionBuilder.run {
            val parts = mutableListOf<Op<Boolean>>()
            parts.add(MagazinesTable.status eq MagazineStatus.APPROVED)
            if (category != null) {
                parts.add(MagazinesTable.categoryId eq category)
            }
            if (!search.isNullOrBlank()) {
                val pattern = "%${search.trim()}%"
                parts.add(MagazinesTable.title ilike pattern or (MagazinesTable.description ilike pattern))
            }
            parts.reduce { acc, op -> acc and op }
        }

        val total = MagazinesTable.select { condition }.count()

        val items = MagazinesTable
            .select { condition }
            .orderBy(MagazinesTable.createdAt to SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1).coerceAtLeast(0).toLong() * pageSize))
            .map { it.toMagazine() }

        PagedResult(items, total)
    }

    override fun findById(id: UUID): Magazine? = transaction {
        MagazinesTable
            .select { MagazinesTable.id eq id }
            .singleOrNull()
            ?.toMagazine()
    }

    override fun findByUploader(userId: UUID): List<Magazine> = transaction {
        MagazinesTable
            .select { MagazinesTable.uploadedBy eq userId }
            .orderBy(MagazinesTable.createdAt to SortOrder.DESC)
            .map { it.toMagazine() }
    }

    override fun findPending(): List<Magazine> = transaction {
        MagazinesTable
            .select { MagazinesTable.status eq MagazineStatus.PENDING }
            .orderBy(MagazinesTable.createdAt to SortOrder.ASC)
            .map { it.toMagazine() }
    }

    override fun create(
        title: String,
        publisher: String?,
        yearFounded: Int?,
        categoryId: Int?,
        description: String?,
        uploadedBy: UUID?,
        status: MagazineStatus,
    ): Magazine = transaction {
        val insertedId = MagazinesTable.insert {
            it[MagazinesTable.title] = title
            it[MagazinesTable.publisher] = publisher
            it[MagazinesTable.yearFounded] = yearFounded
            it[MagazinesTable.categoryId] = categoryId
            it[MagazinesTable.description] = description
            it[MagazinesTable.uploadedBy] = uploadedBy
            it[MagazinesTable.status] = status
        } get MagazinesTable.id

        MagazinesTable
            .select { MagazinesTable.id eq insertedId }
            .single()
            .toMagazine()
    }

    override fun update(
        id: UUID,
        title: String?,
        publisher: String?,
        yearFounded: Int?,
        categoryId: Int?,
        description: String?,
    ): Magazine? = transaction {
        val updated = MagazinesTable.update({ MagazinesTable.id eq id }) { stmt ->
            if (title != null) stmt[MagazinesTable.title] = title
            if (publisher != null) stmt[MagazinesTable.publisher] = publisher
            if (yearFounded != null) stmt[MagazinesTable.yearFounded] = yearFounded
            if (categoryId != null) stmt[MagazinesTable.categoryId] = categoryId
            if (description != null) stmt[MagazinesTable.description] = description
            stmt[MagazinesTable.updatedAt] = CurrentDateTime
        }
        if (updated == 0) null
        else MagazinesTable
            .select { MagazinesTable.id eq id }
            .single()
            .toMagazine()
    }

    override fun updateStatus(id: UUID, status: MagazineStatus): Magazine? = transaction {
        val updated = MagazinesTable.update({ MagazinesTable.id eq id }) { stmt ->
            stmt[MagazinesTable.status] = status
            stmt[MagazinesTable.updatedAt] = CurrentDateTime
        }
        if (updated == 0) null
        else MagazinesTable
            .select { MagazinesTable.id eq id }
            .single()
            .toMagazine()
    }

    override fun updateCoverPath(id: UUID, path: String?): Boolean = transaction {
        MagazinesTable.update({ MagazinesTable.id eq id }) { stmt ->
            stmt[MagazinesTable.coverPath] = path
            stmt[MagazinesTable.updatedAt] = CurrentDateTime
        } > 0
    }

    override fun delete(id: UUID): Boolean = transaction {
        MagazinesTable.deleteWhere { Op.build { MagazinesTable.id eq id } } > 0
    }

    override fun getStats(id: UUID): MagazineStats = transaction {
        val avgExpr = ReviewsTable.rating.avg()
        val countExpr = ReviewsTable.id.count()
        val reviewRow = ReviewsTable
            .slice(avgExpr, countExpr)
            .select { ReviewsTable.magazineId eq id }
            .single()
        val avg = reviewRow[avgExpr]?.toDouble() ?: 0.0
        val reviewsCount = reviewRow[countExpr].toInt()

        val issuesCount = IssuesTable
            .select { IssuesTable.magazineId eq id }
            .count()
            .toInt()

        MagazineStats(
            averageRating = roundTo1(avg),
            reviewsCount = reviewsCount,
            issuesCount = issuesCount,
        )
    }

    override fun getStatsForMany(ids: List<UUID>): Map<UUID, MagazineStats> = transaction {
        if (ids.isEmpty()) return@transaction emptyMap()

        val avgExpr = ReviewsTable.rating.avg()
        val reviewCountExpr = ReviewsTable.id.count()
        val reviewRows = ReviewsTable
            .slice(ReviewsTable.magazineId, avgExpr, reviewCountExpr)
            .select { ReviewsTable.magazineId inList ids }
            .groupBy(ReviewsTable.magazineId)
            .associate { row ->
                row[ReviewsTable.magazineId].value to Pair(
                    row[avgExpr]?.toDouble() ?: 0.0,
                    row[reviewCountExpr].toInt(),
                )
            }

        val issueCountExpr = IssuesTable.id.count()
        val issueRows = IssuesTable
            .slice(IssuesTable.magazineId, issueCountExpr)
            .select { IssuesTable.magazineId inList ids }
            .groupBy(IssuesTable.magazineId)
            .associate { row ->
                row[IssuesTable.magazineId].value to row[issueCountExpr].toInt()
            }

        ids.associateWith { id ->
            val (avg, reviewCount) = reviewRows[id] ?: (0.0 to 0)
            MagazineStats(
                averageRating = roundTo1(avg),
                reviewsCount = reviewCount,
                issuesCount = issueRows[id] ?: 0,
            )
        }
    }

    private fun roundTo1(value: Double): Double =
        BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble()

    private fun ResultRow.toMagazine() = Magazine(
        id = this[MagazinesTable.id].value,
        title = this[MagazinesTable.title],
        publisher = this[MagazinesTable.publisher],
        yearFounded = this[MagazinesTable.yearFounded],
        categoryId = this[MagazinesTable.categoryId]?.value,
        description = this[MagazinesTable.description],
        coverPath = this[MagazinesTable.coverPath],
        uploadedBy = this[MagazinesTable.uploadedBy]?.value,
        status = this[MagazinesTable.status],
        createdAt = this[MagazinesTable.createdAt],
        updatedAt = this[MagazinesTable.updatedAt],
    )
}

private class ILikeOp(
    private val column: Expression<*>,
    private val pattern: Expression<*>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.apply {
            +column
            +" ILIKE "
            +pattern
        }
    }
}

private infix fun Expression<String?>.ilike(pattern: String): Op<Boolean> =
    ILikeOp(this, stringParam(pattern))

@JvmName("ilikeNonNull")
private infix fun Expression<String>.ilike(pattern: String): Op<Boolean> =
    ILikeOp(this, stringParam(pattern))
