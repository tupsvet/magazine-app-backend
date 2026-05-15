package com.magazines.data.repository

import com.magazines.db.tables.ReviewsTable
import com.magazines.db.tables.UsersTable
import com.magazines.domain.model.Review
import java.util.UUID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

interface ReviewRepository {
    fun findByMagazine(magazineId: UUID): List<Review>

    fun findById(id: UUID): Review?

    fun findByUserAndMagazine(userId: UUID, magazineId: UUID): Review?

    fun create(magazineId: UUID, userId: UUID, rating: Int, comment: String?): Review

    fun update(id: UUID, rating: Int?, comment: String?): Review?

    fun delete(id: UUID): Boolean

    fun averageRating(magazineId: UUID): Double?

    fun countByMagazine(magazineId: UUID): Int
}

class ReviewRepositoryImpl : ReviewRepository {

    override fun findByMagazine(magazineId: UUID): List<Review> = transaction {
        ReviewsTable
            .innerJoin(UsersTable, { ReviewsTable.userId }, { UsersTable.id })
            .select { ReviewsTable.magazineId eq magazineId }
            .orderBy(ReviewsTable.createdAt to SortOrder.DESC)
            .map { it.toReviewWithUser() }
    }

    override fun findById(id: UUID): Review? = transaction {
        ReviewsTable
            .innerJoin(UsersTable, { ReviewsTable.userId }, { UsersTable.id })
            .select { ReviewsTable.id eq id }
            .singleOrNull()
            ?.toReviewWithUser()
    }

    override fun findByUserAndMagazine(userId: UUID, magazineId: UUID): Review? = transaction {
        ReviewsTable
            .innerJoin(UsersTable, { ReviewsTable.userId }, { UsersTable.id })
            .select {
                (ReviewsTable.userId eq userId) and (ReviewsTable.magazineId eq magazineId)
            }
            .singleOrNull()
            ?.toReviewWithUser()
    }

    override fun create(
        magazineId: UUID,
        userId: UUID,
        rating: Int,
        comment: String?,
    ): Review = transaction {
        val normalizedComment = comment?.trim()?.takeIf { it.isNotEmpty() }

        val insertedId = ReviewsTable.insert {
            it[ReviewsTable.magazineId] = magazineId
            it[ReviewsTable.userId] = userId
            it[ReviewsTable.rating] = rating
            it[ReviewsTable.comment] = normalizedComment
        } get ReviewsTable.id

        ReviewsTable
            .innerJoin(UsersTable, { ReviewsTable.userId }, { UsersTable.id })
            .select { ReviewsTable.id eq insertedId }
            .single()
            .toReviewWithUser()
    }

    override fun update(id: UUID, rating: Int?, comment: String?): Review? = transaction {
        val updated = ReviewsTable.update({ ReviewsTable.id eq id }) { stmt ->
            if (rating != null) stmt[ReviewsTable.rating] = rating
            if (comment != null) {
                stmt[ReviewsTable.comment] = comment.trim().takeIf { it.isNotEmpty() }
            }
        }
        if (updated == 0) null else findById(id)
    }

    override fun delete(id: UUID): Boolean = transaction {
        ReviewsTable.deleteWhere { Op.build { ReviewsTable.id eq id } } > 0
    }

    override fun averageRating(magazineId: UUID): Double? = transaction {
        val avgExpr = ReviewsTable.rating.avg()
        val row = ReviewsTable
            .slice(avgExpr)
            .select { ReviewsTable.magazineId eq magazineId }
            .single()
        row[avgExpr]?.toDouble()
    }

    override fun countByMagazine(magazineId: UUID): Int = transaction {
        ReviewsTable
            .select { ReviewsTable.magazineId eq magazineId }
            .count()
            .toInt()
    }

    private fun ResultRow.toReviewWithUser() = Review(
        id = this[ReviewsTable.id].value,
        magazineId = this[ReviewsTable.magazineId].value,
        userId = this[ReviewsTable.userId].value,
        userName = resolveUserName(this[UsersTable.displayName], this[UsersTable.email]),
        rating = this[ReviewsTable.rating],
        comment = this[ReviewsTable.comment],
        createdAt = this[ReviewsTable.createdAt],
    )

    private fun resolveUserName(displayName: String?, email: String): String =
        displayName?.trim()?.takeIf { it.isNotEmpty() } ?: email
}
