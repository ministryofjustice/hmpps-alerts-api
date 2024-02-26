package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${user-management.api.url}") private val userManagementApiUri: String,
  @Value("\${prisoner.search.api.url}") private val prisonerSearchApiUri: String,
) {

  @Bean
  fun userManagementWebClient(webclientBuilder: WebClient.Builder): WebClient {
    return webclientBuilder
      .baseUrl(userManagementApiUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun prisonerSearchWebClient(webclientBuilder: WebClient.Builder): WebClient {
    return webclientBuilder
      .baseUrl(prisonerSearchApiUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val authenticationToken: Jwt = SecurityContextHolder.getContext()
        .authentication
        .credentials as Jwt
      val tokenString: String = authenticationToken.tokenValue
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, "Bearer $tokenString")
        .build()
      next.exchange(filtered)
    }
  }
}
