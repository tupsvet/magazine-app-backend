package com.magazines.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.util.UUID

class JwtConfig {

    private val config = ConfigFactory.load().getConfig("jwt")

    val secret: String = config.getString("secret")
    val issuer: String = config.getString("issuer")
    val audience: String = config.getString("audience")
    val realm: String = config.getString("realm")

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    private val validityMs: Long = 24L * 60L * 60L * 1000L

    fun makeToken(userId: UUID, email: String, role: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
            .sign(algorithm)

    fun verifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
}
