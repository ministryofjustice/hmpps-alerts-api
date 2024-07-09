package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import java.util.Optional

class RetrieveAlertCodeIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alert-codes")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alert-codes")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.get()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get alert codes excludes inactive codes by default`() {
    val alertCodes = webTestClient.getAlertCodes(null)
    assertThat(alertCodes.all { it.isActive }).isTrue()
    assertThat(alertCodes.all { it.isActive }).isTrue()
  }

  @Test
  fun `get alert codes including inactive codes`() {
    val alertCodes = webTestClient.getAlertCodes(true)
    assertThat(alertCodes.any { !it.isActive }).isTrue()
    assertThat(alertCodes.any { !it.isActive }).isTrue()
  }

  @Test
  fun `get specific alert code`() {
    val alertCodes = webTestClient.getAlertCode("VI")
    assertThat(alertCodes.code).isEqualTo("VI")
  }

  private fun WebTestClient.getAlertCodes(
    includeInactive: Boolean?,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/alert-codes")
          .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive))
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AlertCode::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getAlertCode(
    alertCode: String,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/alert-codes/$alertCode")
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
}
