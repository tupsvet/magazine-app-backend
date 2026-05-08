package com.magazines

import com.magazines.config.initDatabase
import com.magazines.plugins.configureAuthentication
import com.magazines.plugins.configureRouting
import com.magazines.plugins.configureSerialization
import com.magazines.plugins.configureStatusPages
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    val port = config.property("ktor.deployment.port").getString().toInt()

    initDatabase()

    embeddedServer(Netty, port = port) {
        configureSerialization()
        configureAuthentication()
        configureStatusPages()
        configureRouting()
    }.start(wait = true)
}
