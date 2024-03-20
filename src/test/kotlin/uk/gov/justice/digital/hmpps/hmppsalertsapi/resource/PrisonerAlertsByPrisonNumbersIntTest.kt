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
  fun `403 forbidden - alerts writer`() {
    webTestClient.get()
      .uri("/prisoners/alerts?prisonNumbers=A1234AA")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no prison numbers`() {
    val response = webTestClient.get()
      .uri("/prisoners/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Required request parameter 'prisonNumbers' for method parameter type Collection is not present")
      assertThat(developerMessage).isEqualTo("Required request parameter 'prisonNumbers' for method parameter type Collection is not present")
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
    webTestClient.get()
      .uri { builder ->
        builder
          .path("/prisoners/alerts")
          .queryParam("prisonNumbers", prisonNumbers)
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectBody(PrisonersAlerts::class.java)
      .returnResult().responseBody!!
}
