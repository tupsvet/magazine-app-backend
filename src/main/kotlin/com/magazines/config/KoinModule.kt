package com.magazines.config

import com.magazines.data.repository.CategoryRepository
import com.magazines.data.repository.CategoryRepositoryImpl
import com.magazines.data.repository.FavoriteRepository
import com.magazines.data.repository.FavoriteRepositoryImpl
import com.magazines.data.repository.IssueRepository
import com.magazines.data.repository.IssueRepositoryImpl
import com.magazines.data.repository.MagazineRepository
import com.magazines.data.repository.MagazineRepositoryImpl
import com.magazines.data.repository.ReviewRepository
import com.magazines.data.repository.ReviewRepositoryImpl
import com.magazines.data.repository.UserRepository
import com.magazines.service.AuthService
import com.magazines.service.CategoryService
import com.magazines.service.FavoriteService
import com.magazines.service.FileStorageService
import com.magazines.service.IssueService
import com.magazines.service.MagazineService
import com.magazines.service.ModerationService
import com.magazines.service.PasswordHasher
import com.magazines.service.ReviewService
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

    single(named("storagePath")) {
        runCatching { ConfigFactory.load().getString("storage.path") }
            .getOrDefault("./storage")
    }

    single {
        FileStorageService(get(named("storagePath")))
    }

    single { UserRepository() }
    single<CategoryRepository> { CategoryRepositoryImpl() }
    single<MagazineRepository> { MagazineRepositoryImpl() }
    single<IssueRepository> { IssueRepositoryImpl() }
    single<ReviewRepository> { ReviewRepositoryImpl() }
    single<FavoriteRepository> { FavoriteRepositoryImpl() }

    single { AuthService(get(), get(), get()) }
    single { CategoryService(get()) }
    single { MagazineService(get(), get(), get(), get(named("baseUrl"))) }
    single { IssueService(get(), get(), get(), get(), get(named("baseUrl"))) }
    single { ReviewService(get(), get()) }
    single { FavoriteService(get(), get(), get(), get(named("baseUrl"))) }
    single { ModerationService(get()) }
}
