package com.magazines.util

import com.magazines.domain.exception.ForbiddenException
import com.magazines.domain.model.User
import com.magazines.domain.model.UserRole

fun requireAdmin(user: User) {
    if (user.role != UserRole.ADMIN) {
        throw ForbiddenException("Admin role required")
    }
}
