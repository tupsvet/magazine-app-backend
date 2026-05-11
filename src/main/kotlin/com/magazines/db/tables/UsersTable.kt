package com.magazines.db.tables

import com.magazines.domain.model.UserRole
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 100).nullable()
    val role = varchar("role", 20)
        .check("valid_user_role") {
            it inList listOf("USER", "ADMIN")
        }
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
