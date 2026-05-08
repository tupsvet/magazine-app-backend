package com.magazines.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ReviewsTable : UUIDTable("reviews") {
    val magazineId = reference("magazine_id", MagazinesTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable)
    val rating = integer("rating").check { it greaterEq 1 and (it lessEq 5) }
    val comment = text("comment").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(magazineId, userId)
    }
}
