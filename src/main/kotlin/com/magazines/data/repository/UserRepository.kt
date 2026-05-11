package com.magazines.data.repository

import com.magazines.db.tables.UsersTable
import com.magazines.domain.model.User
import com.magazines.domain.model.UserRole
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepository {

    suspend fun findByEmail(email: String): User? = newSuspendedTransaction {
        UsersTable
            .select { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun findById(id: UUID): User? = newSuspendedTransaction {
        UsersTable
            .select { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun create(
        email: String,
        passwordHash: String,
        displayName: String?,
        role: UserRole = UserRole.USER,
    ): User = newSuspendedTransaction {
        val insertedId = UsersTable.insert {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.displayName] = displayName
            it[UsersTable.role] = role
        } get UsersTable.id

        UsersTable
            .select { UsersTable.id eq insertedId }
            .single()
            .toUser()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        displayName = this[UsersTable.displayName],
        role = this[UsersTable.role],
        createdAt = this[UsersTable.createdAt],
    )
}
