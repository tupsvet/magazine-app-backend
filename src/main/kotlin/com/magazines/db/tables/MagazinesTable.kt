package com.magazines.db.tables

import com.magazines.domain.model.MagazineStatus
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object MagazinesTable : UUIDTable("magazines") {
    val title = varchar("title", 255)
    val publisher = varchar("publisher", 255).nullable()
    val yearFounded = integer("year_founded").nullable()
    val categoryId = reference("category_id", CategoriesTable).nullable()
    val description = text("description").nullable()
    val coverPath = varchar("cover_path", 500).nullable()
    val uploadedBy = reference("uploaded_by", UsersTable).nullable()
    val status = enumerationByName("status", 20, MagazineStatus::class)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
