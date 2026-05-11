package com.magazines

import com.magazines.config.appModule
import com.magazines.config.initDatabase
import com.magazines.plugins.configureAuthentication
import com.magazines.plugins.configureRouting
import com.magazines.plugins.configureSerialization
import com.magazines.plugins.configureStatusPages
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.install
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    val port = config.property("ktor.deployment.port").getString().toInt()

    initDatabase()

    embeddedServer(Netty, port = port) {
        install(Koin) {
            slf4jLogger()
            modules(appModule)
        }
        configureSerialization()
        configureAuthentication()
        configureStatusPages()
        configureRouting()
    }.start(wait = true)
}
