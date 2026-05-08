package com.magazines.config

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

private var hikariDataSource: HikariDataSource? = null

val dataSource: DataSource
    get() = hikariDataSource ?: error("Database is not initialized. Call initDatabase() first.")

fun initDatabase() {
    val dbConfig = ConfigFactory.load().getConfig("database")

    val ds = HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.getString("url")
        username = dbConfig.getString("user")
        password = dbConfig.getString("password")
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })
    hikariDataSource = ds

    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(ds)
}
