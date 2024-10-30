package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.channel.ChannelOption.SO_KEEPALIVE
import io.netty.channel.epoll.EpollChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClient.create
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @Value("\${api.base.url.manage-users}") private val manageUsersBaseUri: String,
  @Value("\${api.base.url.prisoner-search}") private val prisonerSearchBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:2s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: Builder): WebClient =
    builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun manageUsersHealthWebClient(builder: Builder): WebClient =
    builder.healthWebClient(manageUsersBaseUri, healthTimeout)

  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) =
    getOAuthWebClient(authorizedClientManager, builder, manageUsersBaseUri)

  @Bean
  fun prisonerSearchHealthWebClient(builder: Builder): WebClient =
    builder.healthWebClient(prisonerSearchBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) =
    getOAuthWebClient(authorizedClientManager, builder, prisonerSearchBaseUri)

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("default")
    return builder.baseUrl(rootUri)
      .clientConnector(clientConnector())
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  private fun clientConnector(consumer: ((HttpClient) -> Unit)? = null): ReactorClientHttpConnector {
    val client = create().responseTimeout(timeout)
      .option(CONNECT_TIMEOUT_MILLIS, 1000)
      .option(SO_KEEPALIVE, true)
      // this will show a warning on apple (arm) architecture but will work on linux x86 container
      .option(EpollChannelOption.TCP_KEEPINTVL, 60)
    consumer?.also { it.invoke(client) }
    return ReactorClientHttpConnector(client)
  }
}
