package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertsResponse
import java.time.LocalDate.now

class PrisonerAlertsByPrisonNumbersIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri(SEARCH_URL)
      .bodyValue(setOf("A1234AA"))
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri(SEARCH_URL)
      .bodyValue(setOf("A1234AA"))
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no prison numbers`() {
    val response = webTestClient.post()
      .uri(SEARCH_URL)
      .bodyValue(emptySet<String>())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison numbers must not be empty")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: Prison numbers must not be empty")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `get prisoners alerts`() {
    val p1 = givenPrisoner()
    val p2 = givenPrisoner()
    val p3 = givenPrisoner()
    val codes1 = alertCodeRepository.findAll().shuffled().take(2)
    val codes2 = alertCodeRepository.findAll().shuffled().take(5)
    val codes3 = alertCodeRepository.findAll().shuffled().take(2)
    codes1.forEach { givenAlert(alert(p1, it)) }
    codes2.forEachIndexed { i, a ->
      givenAlert(alert(p2, a, activeTo = if (i % 3 != 0) null else now().minusDays(1)))
    }
    codes3.forEach { givenAlert(alert(p3, it)) }

    val prisonersAlerts = getPrisonerAlerts(setOf(p1, p2, PRISON_NUMBER_NOT_FOUND))

    with(prisonersAlerts) {
      assertThat(map { it.prisonNumber }.toSet()).containsExactlyInAnyOrderElementsOf(setOf(p1, p2))
      Assertions.assertTrue(all { it.isActive })
      assertThat(count { it.prisonNumber == p1 }).isEqualTo(2)
      assertThat(count { it.prisonNumber == p2 }).isEqualTo(3)
    }
  }

  @Test
  fun `get prisoners alerts including inactive`() {
    val p1 = givenPrisoner()
    val p2 = givenPrisoner()
    val codes1 = alertCodeRepository.findAll().shuffled().take(3)
    val codes2 = alertCodeRepository.findAll().shuffled().take(7)
    codes1.forEach { givenAlert(alert(p1, it)) }
    codes2.forEachIndexed { i, a ->
      givenAlert(alert(p2, a, activeTo = if (i % 3 != 0) null else now().minusDays(1)))
    }

    val prisonersAlerts = getPrisonerAlerts(setOf(p1, p2, PRISON_NUMBER_NOT_FOUND), true)

    with(prisonersAlerts) {
      assertThat(map { it.prisonNumber }.toSet()).containsExactlyInAnyOrderElementsOf(setOf(p1, p2))
      assertThat(map { it.isActive }.toSet()).containsExactlyInAnyOrderElementsOf(setOf(true, false))
      assertThat(count { it.prisonNumber == p1 }).isEqualTo(3)
      assertThat(count { it.prisonNumber == p2 }).isEqualTo(7)
    }
  }

  private fun getPrisonerAlerts(prisonNumbers: Set<String>, includeInactive: Boolean = false): List<Alert> = webTestClient.post()
    .uri {
      it.path(SEARCH_URL)
      if (includeInactive) it.queryParam("includeInactive", "true")
      it.build()
    }
    .bodyValue(prisonNumbers)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
    .exchange()
    .expectStatus().isOk
    .expectBody<AlertsResponse>()
    .returnResult().responseBody!!.content

  companion object {
    const val SEARCH_URL = "/search/alerts/prison-numbers"
  }
}
