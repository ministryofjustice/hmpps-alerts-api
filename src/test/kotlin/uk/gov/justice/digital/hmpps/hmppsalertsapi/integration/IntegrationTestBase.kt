package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.OAuthExtension

@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql("classpath:test_data/reset-database.sql")
@ExtendWith(OAuthExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisation(
    user: String? = null,
    client: String = CLIENT_ID,
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, client, roles)

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
