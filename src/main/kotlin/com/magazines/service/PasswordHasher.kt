package com.magazines.service

import org.mindrot.jbcrypt.BCrypt

class PasswordHasher {

    fun hash(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(password: String, hash: String): Boolean =
        try {
            BCrypt.checkpw(password, hash)
        } catch (e: IllegalArgumentException) {
            false
        }
}
