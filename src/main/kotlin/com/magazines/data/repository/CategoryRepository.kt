package com.magazines.data.repository

import com.magazines.db.tables.CategoriesTable
import com.magazines.domain.model.Category
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

interface CategoryRepository {
    fun findAll(): List<Category>
    fun findById(id: Int): Category?
    fun findByName(name: String): Category?
    fun create(name: String, description: String?): Category
    fun update(id: Int, name: String?, description: String?): Category?
    fun delete(id: Int): Boolean
}

class CategoryRepositoryImpl : CategoryRepository {

    override fun findAll(): List<Category> = transaction {
        CategoriesTable
            .selectAll()
            .orderBy(CategoriesTable.name)
            .map { it.toCategory() }
    }

    override fun findById(id: Int): Category? = transaction {
        CategoriesTable
            .select { CategoriesTable.id eq id }
            .singleOrNull()
            ?.toCategory()
    }

    override fun findByName(name: String): Category? = transaction {
        CategoriesTable
            .select { CategoriesTable.name eq name }
            .singleOrNull()
            ?.toCategory()
    }

    override fun create(name: String, description: String?): Category = transaction {
        val insertedId = CategoriesTable.insert {
            it[CategoriesTable.name] = name
            it[CategoriesTable.description] = description
        } get CategoriesTable.id

        CategoriesTable
            .select { CategoriesTable.id eq insertedId }
            .single()
            .toCategory()
    }

    override fun update(id: Int, name: String?, description: String?): Category? = transaction {
        val updated = CategoriesTable.update({ CategoriesTable.id eq id }) { stmt ->
            if (name != null) stmt[CategoriesTable.name] = name
            if (description != null) stmt[CategoriesTable.description] = description
        }
        if (updated == 0) {
            null
        } else {
            CategoriesTable
                .select { CategoriesTable.id eq id }
                .single()
                .toCategory()
        }
    }

    override fun delete(id: Int): Boolean = transaction {
        CategoriesTable.deleteWhere { Op.build { CategoriesTable.id eq id } } > 0
    }

    private fun ResultRow.toCategory() = Category(
        id = this[CategoriesTable.id].value,
        name = this[CategoriesTable.name],
        description = this[CategoriesTable.description],
    )
}
