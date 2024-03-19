package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response.PrisonersAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class PrisonerAlertsByPrisonNumbersIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

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
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response.PrisonersAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.PrisonerAlertsController.retrievePrisonerAlerts(java.util.Collection<java.lang.String>)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/prisoner-alerts-for-multiple-prison-numbers.sql")
  fun `get prisoners alerts`() {
    val prisonNumbers = listOf("A1234AA", "B2345BB", PRISON_NUMBER_NOT_FOUND)

    val prisonersAlerts = getPrisonerAlerts(prisonNumbers)

    with(prisonersAlerts) {
      assertOnlyContainsAlertsForPrisonNumbers(listOf("A1234AA", "B2345BB"))
      assertAlertCountForPrisonNumber("A1234AA", 2)
      assertAlertCountForPrisonNumber("B2345BB", 3)
      assertDoesNotContainDeletedAlert()
      assertOrderedByActiveFromDesc()
    }
  }

  private fun PrisonersAlerts.assertOnlyContainsAlertsForPrisonNumbers(prisonNumbers: List<String>) {
    assertThat(prisonNumbers).isEqualTo(prisonNumbers)
    assertThat(alerts.map { it.prisonNumber }.distinct()).isEqualTo(prisonNumbers)
  }

  private fun PrisonersAlerts.assertAlertCountForPrisonNumber(prisonNumber: String, expectedCount: Int) =
    assertThat(alerts.filter { it.prisonNumber == prisonNumber }).hasSize(expectedCount)

  private fun PrisonersAlerts.assertDoesNotContainDeletedAlert() =
    alertRepository.findByAlertUuidIncludingSoftDelete(deletedAlertUuid)!!.also { deletedAlert ->
      assertThat(alerts.firstOrNull { it.prisonNumber == deletedAlert.prisonNumber }).isNotNull
      assertThat(alerts.none { it.alertUuid == deletedAlert.alertUuid }).isTrue
    }

  private fun PrisonersAlerts.assertOrderedByActiveFromDesc() =
    assertThat(alerts).isSortedAccordingTo(compareByDescending { it.activeFrom })

  private fun getPrisonerAlerts(prisonNumbers: List<String>) =
    webTestClient.post()
      .uri("/prisoners/alerts")
      .bodyValue(prisonNumbers)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectBody(PrisonersAlerts::class.java)
      .returnResult().responseBody!!
}
