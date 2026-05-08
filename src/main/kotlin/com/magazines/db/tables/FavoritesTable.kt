package com.magazines.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object FavoritesTable : Table("favorites") {
    val userId = reference("user_id", UsersTable)
    val magazineId = reference("magazine_id", MagazinesTable, onDelete = ReferenceOption.CASCADE)
    val addedAt = datetime("added_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, magazineId)
}
