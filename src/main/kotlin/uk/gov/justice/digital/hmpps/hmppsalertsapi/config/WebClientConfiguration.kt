package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.base.url.manage-users}") private val manageUsersBaseUri: String,
  @param:Value("\${api.base.url.prisoner-search}") private val prisonerSearchBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:2s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun manageUsersHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(manageUsersBaseUri, healthTimeout)

  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = builder.authorisedWebClient(authorizedClientManager, "default", manageUsersBaseUri, timeout)

  @Bean
  fun prisonerSearchHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonerSearchBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = builder.authorisedWebClient(authorizedClientManager, "default", prisonerSearchBaseUri, timeout)
}
