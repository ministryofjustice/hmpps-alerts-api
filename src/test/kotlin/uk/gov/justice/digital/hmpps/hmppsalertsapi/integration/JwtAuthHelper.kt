package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.SIG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

internal const val CLIENT_ID = "alerts-api-client"

@Component
class JwtAuthHelper {
  private lateinit var keyPair: KeyPair

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Profile("test")
  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  fun setAuthorisation(
    user: String? = null,
    client: String = CLIENT_ID,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
    isUserToken: Boolean = true,
  ): (HttpHeaders) -> Unit {
    val token = createJwt(
      subject = user ?: client,
      user = user,
      client = client,
      scope = scopes,
      expiryTime = Duration.ofHours(1L),
      roles = roles,
      isUserToken = isUserToken,
    )
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  internal fun createJwt(
    subject: String,
    user: String?,
    client: String?,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
    isUserToken: Boolean = true,
  ): String = mutableMapOf<String, Any>()
    .also {
      user?.let { user ->
        it[
          when (isUserToken) {
            true -> "user_name" false -> "username"
          },
        ] = user
      }
    }
    .also { client?.let { client -> it["client_id"] = client } }
    .also { roles?.let { roles -> it["authorities"] = roles } }
    .also { scope?.let { scope -> it["scope"] = scope } }
    .let {
      Jwts.builder()
        .id(jwtId)
        .subject(subject)
        .claims(it.toMap())
        .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
        .signWith(keyPair.private, SIG.RS256)
        .compact()
    }
}
