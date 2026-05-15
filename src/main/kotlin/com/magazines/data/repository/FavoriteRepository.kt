package com.magazines.data.repository

import com.magazines.db.tables.FavoritesTable
import com.magazines.db.tables.MagazinesTable
import com.magazines.domain.model.Magazine
import com.magazines.domain.model.MagazineStatus
import java.util.UUID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface FavoriteRepository {
    fun findByUser(userId: UUID): List<Magazine>

    fun exists(userId: UUID, magazineId: UUID): Boolean

    fun add(userId: UUID, magazineId: UUID): Boolean

    fun remove(userId: UUID, magazineId: UUID): Boolean
}

class FavoriteRepositoryImpl : FavoriteRepository {

    override fun findByUser(userId: UUID): List<Magazine> = transaction {
        FavoritesTable
            .innerJoin(MagazinesTable, { FavoritesTable.magazineId }, { MagazinesTable.id })
            .select {
                (FavoritesTable.userId eq userId) and (MagazinesTable.status eq MagazineStatus.APPROVED)
            }
            .orderBy(FavoritesTable.addedAt to SortOrder.DESC)
            .map { it.toMagazine() }
    }

    override fun exists(userId: UUID, magazineId: UUID): Boolean = transaction {
        FavoritesTable
            .select {
                (FavoritesTable.userId eq userId) and (FavoritesTable.magazineId eq magazineId)
            }
            .any()
    }

    override fun add(userId: UUID, magazineId: UUID): Boolean = transaction {
        val alreadyExists = FavoritesTable
            .select {
                (FavoritesTable.userId eq userId) and (FavoritesTable.magazineId eq magazineId)
            }
            .any()
        if (alreadyExists) return@transaction false

        FavoritesTable.insert {
            it[FavoritesTable.userId] = userId
            it[FavoritesTable.magazineId] = magazineId
        }
        true
    }

    override fun remove(userId: UUID, magazineId: UUID): Boolean = transaction {
        FavoritesTable.deleteWhere {
            Op.build {
                (FavoritesTable.userId eq userId) and (FavoritesTable.magazineId eq magazineId)
            }
        } > 0
    }

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
