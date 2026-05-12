package com.magazines.config

import com.magazines.data.repository.CategoryRepository
import com.magazines.data.repository.CategoryRepositoryImpl
import com.magazines.data.repository.MagazineRepository
import com.magazines.data.repository.MagazineRepositoryImpl
import com.magazines.data.repository.UserRepository
import com.magazines.service.AuthService
import com.magazines.service.CategoryService
import com.magazines.service.MagazineService
import com.magazines.service.PasswordHasher
import com.typesafe.config.ConfigFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { JwtConfig() }
    single { PasswordHasher() }

    single(named("baseUrl")) {
        runCatching { ConfigFactory.load().getString("server.baseUrl") }
            .getOrDefault("http://localhost:8080")
    }

    single { UserRepository() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<MagazineRepository> { MagazineRepositoryImpl() }

    single { AuthService(get(), get(), get()) }
    single { CategoryService(get()) }
    single { MagazineService(get(), get(), get(named("baseUrl"))) }
}
