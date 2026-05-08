package com.magazines.db.tables

import com.magazines.domain.model.UserRole
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UsersTable : UUIDTable("users") {
    val firebaseUid = varchar("firebase_uid", 128).uniqueIndex()
    val email = varchar("email", 255)
    val displayName = varchar("display_name", 100).nullable()
    val role = enumerationByName("role", 20, UserRole::class)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
