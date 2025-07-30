package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.time.Duration

@Configuration
class Oauth2Configuration(
  @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") val jwkSetUri: String,
  @Value("\${api.auth.connect-timeout:2s}") val connectTimeout: Duration,
  @Value("\${api.auth.read-timeout:2s}") val readTimeout: Duration,
) {
  @Profile("!test")
  @Bean
  fun JwtDecoder(builder: RestTemplateBuilder): JwtDecoder {
    val rest = builder.connectTimeout(connectTimeout).readTimeout(readTimeout)
      .build()
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).restOperations(rest).build()
  }
}
