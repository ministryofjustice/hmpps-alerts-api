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
  fun `400 bad request - empty code filter`() {
    val response = webTestClient.post()
      .uri {
        it.path(SEARCH_URL)
        it.queryParam("filterAlertCodes", "")
        it.build()
      }
      .bodyValue(setOf("A1234AA"))
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: When provided, filterAlertCodes must include at least one code")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: When provided, filterAlertCodes must include at least one code")
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

  @Test
  fun `get filtered prisoners alerts`() {
    val p1 = givenPrisoner()
    val p2 = givenPrisoner()
    val p3 = givenPrisoner()
    val (code1, code2, code3) = alertCodeRepository.findAll().shuffled().take(3)
    givenAlert(alert(p1, code1))
    givenAlert(alert(p2, code2))
    givenAlert(alert(p3, code3))

    val prisonersAlerts = getPrisonerAlerts(setOf(p1, p2, p3), filterAlertCodes = setOf(code1.code, code3.code))

    with(prisonersAlerts) {
      assertThat(size).isEqualTo(2)
      assertThat(find { it.prisonNumber == p1 }!!.alertCode.code).isEqualTo(code1.code)
      assertThat(find { it.prisonNumber == p3 }!!.alertCode.code).isEqualTo(code3.code)
    }
  }

  private fun getPrisonerAlerts(
    prisonNumbers: Set<String>,
    includeInactive: Boolean = false,
    filterAlertCodes: Set<String>? = null,
  ): List<Alert> = webTestClient.post()
    .uri {
      it.path(SEARCH_URL)
      if (includeInactive) {
        it.queryParam("includeInactive", "true")
      }
      filterAlertCodes?.forEach { code ->
        it.queryParam("filterAlertCodes", code)
      }
      it.build()
    }
    .bodyValue(prisonNumbers)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
    .headers(setAlertRequestContext())
    .exchange()
    .expectStatus().isOk
    .expectBody<AlertsResponse>()
    .returnResult().responseBody!!.content

  companion object {
    const val SEARCH_URL = "/search/alerts/prison-numbers"
  }
}
