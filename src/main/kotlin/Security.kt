package com.magazines

import io.ktor.server.application.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.io.File
import com.kborowy.authprovider.firebase.firebase

fun Application.configureSecurity() {
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ktor sample app"
    val jwtSecret = "secret"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
    val myAdminFile = File("firebase-adminsdk.json")
    if (!myAdminFile.exists()) return

    install(Authentication) {
        firebase {
            setup {
                adminFile = myAdminFile
            }
            realm = "My Server"
            validate { token ->
                MyAuthenticatedUser(id = token.uid)
            }
        }
    }
}