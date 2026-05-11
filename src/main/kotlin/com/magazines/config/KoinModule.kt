package com.magazines.config

import com.magazines.data.repository.UserRepository
import com.magazines.service.AuthService
import com.magazines.service.PasswordHasher
import org.koin.dsl.module

val appModule = module {
    single { JwtConfig() }
    single { PasswordHasher() }
    single { UserRepository() }
    single { AuthService(get(), get(), get()) }
}
