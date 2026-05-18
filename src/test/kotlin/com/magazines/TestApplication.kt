package com.magazines

import com.magazines.config.JwtConfig
import com.magazines.domain.model.User
import com.magazines.module
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import org.junit.jupiter.api.AfterEach
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

object TestApplication {

    private const val LOCAL_JDBC_URL = "jdbc:postgresql://localhost:5432/magazines_catalog"
    private const val LOCAL_USER = "app"
    private const val LOCAL_PASSWORD = "app_password"

    private enum class DatabaseSource { ENV, LOCAL_COMPOSE, TESTCONTAINERS }

    private val databaseSource: DatabaseSource by lazy { resolveDatabaseSource() }

    private val postgresContainer: PostgreSQLContainer<*>? by lazy {
        if (databaseSource != DatabaseSource.TESTCONTAINERS) return@lazy null
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("magazines_test")
            .withUsername("test")
            .withPassword("test")
    }

    val jwtConfig: JwtConfig by lazy { JwtConfig() }

    fun makeToken(userId: UUID, email: String, role: String): String =
        jwtConfig.makeToken(userId, email, role)

    fun makeToken(user: User): String =
        makeToken(user.id, user.email, user.role.name)

    fun authHeader(user: User): Headers =
        headersOf(HttpHeaders.Authorization, "Bearer ${makeToken(user)}")

    fun runTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        stopKoin()
        ensureDatabaseReady()
        runBlocking {
            testApplication {
                environment {
                    developmentMode = false
                }
                application { module() }
                block()
            }
        }
        stopKoin()
    }

    fun jdbcConnection(): Connection {
        ensureDatabaseReady()
        return DriverManager.getConnection(
            System.getProperty("DATABASE_URL"),
            System.getProperty("DATABASE_USER"),
            System.getProperty("DATABASE_PASSWORD"),
        )
    }

    fun ensureDatabaseReady() {
        when (databaseSource) {
            DatabaseSource.ENV, DatabaseSource.LOCAL_COMPOSE -> Unit
            DatabaseSource.TESTCONTAINERS -> {
                val container = postgresContainer!!
                if (!container.isRunning) {
                    container.start()
                    applyDatabaseProperties(container.jdbcUrl, container.username, container.password)
                }
            }
        }
    }

    private fun resolveDatabaseSource(): DatabaseSource {
        System.getenv("TEST_DATABASE_URL")?.let { url ->
            applyDatabaseProperties(
                url,
                System.getenv("TEST_DATABASE_USER") ?: LOCAL_USER,
                System.getenv("TEST_DATABASE_PASSWORD") ?: LOCAL_PASSWORD,
            )
            return DatabaseSource.ENV
        }

        if (canConnect(LOCAL_JDBC_URL, LOCAL_USER, LOCAL_PASSWORD)) {
            applyDatabaseProperties(LOCAL_JDBC_URL, LOCAL_USER, LOCAL_PASSWORD)
            return DatabaseSource.LOCAL_COMPOSE
        }

        return DatabaseSource.TESTCONTAINERS
    }

    private fun canConnect(url: String, user: String, password: String): Boolean =
        runCatching {
            DriverManager.getConnection(url, user, password).use { it.isValid(2) }
        }.getOrDefault(false)

    private fun applyDatabaseProperties(url: String, user: String, password: String) {
        System.setProperty("DATABASE_URL", url)
        System.setProperty("DATABASE_USER", user)
        System.setProperty("DATABASE_PASSWORD", password)
    }

    private fun applyDatabaseProperties(container: PostgreSQLContainer<*>) {
        applyDatabaseProperties(container.jdbcUrl, container.username, container.password)
    }
}

abstract class IntegrationTestBase {

    @AfterEach
    fun stopKoinAfterTest() {
        stopKoin()
    }

    protected val jwtConfig get() = TestApplication.jwtConfig

    protected fun makeToken(userId: UUID, email: String, role: String): String =
        TestApplication.makeToken(userId, email, role)

    protected fun makeToken(user: User): String =
        TestApplication.makeToken(user)

    protected fun authHeader(user: User): Headers =
        TestApplication.authHeader(user)

    protected fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) =
        TestApplication.runTestApplication(block)

    protected fun promoteToAdmin(email: String) {
        val normalized = email.trim().lowercase()
        TestApplication.jdbcConnection().use { conn ->
            conn.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE email = ?").use { stmt ->
                stmt.setString(1, normalized)
                check(stmt.executeUpdate() == 1) { "Failed to promote user to admin: $normalized" }
            }
        }
    }
}
