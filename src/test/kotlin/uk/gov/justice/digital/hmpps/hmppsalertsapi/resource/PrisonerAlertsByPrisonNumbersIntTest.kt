package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.BAD_REQUEST
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert

class PrisonerAlertsByPrisonNumbersIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/prisoners/alerts?prisonNumbers=A1234AA")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/prisoners/alerts?prisonNumbers=A1234AA")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no prison numbers`() {
    val response = webTestClient.get()
      .uri("/prisoners/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Required request parameter 'prisonNumbers' for method parameter type Collection is not present")
      assertThat(developerMessage).isEqualTo("Required request parameter 'prisonNumbers' for method parameter type Collection is not present")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `get prisoners alerts`() {
    val p1 = givenPrisoner()
    val p2 = givenPrisoner()
    val codes1 = alertCodeRepository.findAll().shuffled().take(2)
    val codes2 = alertCodeRepository.findAll().shuffled().take(3)
    codes1.forEach { givenAlert(alert(p1, it)) }
    codes2.forEach { givenAlert(alert(p2, it)) }

    val prisonersAlerts = getPrisonerAlerts(listOf(p1, p2, PRISON_NUMBER_NOT_FOUND))

    with(prisonersAlerts) {
      assertOnlyContainsAlertsForPrisonNumbers(listOf(p1, p2))
      assertAlertCountForPrisonNumber(p1, 2)
      assertAlertCountForPrisonNumber(p2, 3)
      assertOrderedByActiveFromDesc()
    }
  }

  private fun Map<String, List<Alert>>.prisonNumbers() = keys.toList()
  private fun Map<String, List<Alert>>.alerts() = values.flatten()

  private fun Map<String, List<Alert>>.assertOnlyContainsAlertsForPrisonNumbers(prisonNumbers: List<String>) {
    assertThat(prisonNumbers()).containsExactlyInAnyOrderElementsOf(prisonNumbers)
    assertThat(alerts().map { it.prisonNumber }.distinct()).containsExactlyInAnyOrderElementsOf(prisonNumbers)
  }

  private fun Map<String, List<Alert>>.assertAlertCountForPrisonNumber(prisonNumber: String, expectedCount: Int) {
    assertThat(containsKey(prisonNumber))
    assertThat(this[prisonNumber]!!.filter { it.prisonNumber == prisonNumber }).hasSize(expectedCount)
  }

  private fun Map<String, List<Alert>>.assertOrderedByActiveFromDesc() =
    values.forEach { alerts ->
      assertThat(alerts).isSortedAccordingTo(compareByDescending { it.activeFrom })
    }

  private fun getPrisonerAlerts(prisonNumbers: List<String>) =
    webTestClient.get()
      .uri { builder ->
        builder
          .path("/prisoners/alerts")
          .queryParam("prisonNumbers", prisonNumbers)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange()
      .expectStatus().isOk
      .expectBody(object : ParameterizedTypeReference<Map<String, List<Alert>>>() {})
      .returnResult().responseBody!!
}
