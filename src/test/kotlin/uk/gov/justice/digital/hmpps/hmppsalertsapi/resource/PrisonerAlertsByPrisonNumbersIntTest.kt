package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class PrisonerAlertsByPrisonNumbersIntTest : IntegrationTestBase() {
  private val deletedAlertUuid = UUID.fromString("a2c6af2c-9e70-4fd7-bac3-f3029cfad9b8")

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/prisoners/alerts")
      .bodyValue(emptyList<String>())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/prisoners/alerts")
      .bodyValue(emptyList<String>())
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/prisoners/alerts")
      .bodyValue(emptyList<String>())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/prisoners/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      Assertions.assertThat(status).isEqualTo(400)
      Assertions.assertThat(errorCode).isNull()
      Assertions.assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      Assertions.assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response.PrisonersAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.PrisonerAlertsController.retrievePrisonerAlerts(java.util.Collection<java.lang.String>)")
      Assertions.assertThat(moreInfo).isNull()
    }
  }

  private fun getPrisonerAlerts(prisonNumbers: List<String>) =
    webTestClient.post()
      .uri("/prisoners/alerts")
      .bodyValue(prisonNumbers)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(Alert::class.java)
      .returnResult().responseBody!!
}
