package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill.CaseNoteAlertResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalTime

class AlertsForCaseNotesIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest(prisonNumber())).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get().uri {
      it.path(urlToTest(prisonNumber()))
      it.queryParam("from", now())
      it.queryParam("to", now())
      it.build()
    }.headers(setAuthorisation())
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `get alerts for contacts`() {
    val prisonNumber = givenPrisoner()
    val from = now().minusDays(90)
    val to = now()
    val codes = alertCodeRepository.findAll().shuffled().take(6)
    givenAlert(
      alert(
        prisonNumber,
        codes[0],
        activeFrom = from.minusDays(1),
        createdAt = from.minusDays(2).atStartOfDay(),
      ),
    )
    val createdInRange = givenAlert(
      alert(
        prisonNumber,
        codes[1],
        activeFrom = from,
        createdAt = from.atStartOfDay(),
      ),
    )
    val activeFromInRange = givenAlert(
      alert(
        prisonNumber,
        codes[2],
        activeFrom = from.plusDays(1),
        createdAt = from.minusDays(10).atStartOfDay(),
      ),
    )
    val activeToInRange = givenAlert(
      alert(
        prisonNumber,
        codes[3],
        activeFrom = from.minusDays(1),
        createdAt = from.minusDays(1).atStartOfDay(),
        activeTo = to.minusDays(1),
      ),
    )
    val modifiedInRange = givenAlert(
      alert(
        prisonNumber,
        codes[4],
        activeFrom = from.minusDays(1),
        createdAt = from.minusDays(1).atStartOfDay(),
        activeTo = to.plusDays(1),
      ).update(
        "Updated description",
        null,
        from.plusDays(1),
        null,
        from.plusDays(3).atTime(LocalTime.now()),
        "ANO123",
        "A N Other",
        Source.DPS,
        null,
      ),
    )
    givenAlert(
      alert(
        prisonNumber,
        codes[5],
        activeFrom = to.plusDays(1),
        createdAt = to.plusDays(1).atStartOfDay(),
      ),
    )

    val saved = alertRepository.findByPrisonNumber(prisonNumber)
    assertThat(saved).hasSize(6)
    val res = getAlerts(prisonNumber, from, to)
    with(res.content) {
      assertThat(size).isEqualTo(4)
      val actualCodes = map { it.type.code to it.subType.code }
      val expectedCodes = listOf(createdInRange, activeFromInRange, activeToInRange, modifiedInRange)
        .map { it.alertCode.alertType.code to it.alertCode.code }
      assertThat(actualCodes).containsExactlyInAnyOrderElementsOf(expectedCodes)
    }
  }

  private fun getAlerts(prisonNumber: String, from: LocalDate, to: LocalDate): CaseNoteAlertResponse = webTestClient.get()
    .uri {
      it.path(urlToTest(prisonNumber))
      it.queryParam("from", from)
      it.queryParam("to", to)
      it.build()
    }
    .headers(setAuthorisation(roles = listOf(ROLE_CASE_NOTES)))
    .exchange()
    .expectStatus().isOk
    .expectBody<CaseNoteAlertResponse>()
    .returnResult().responseBody!!

  fun urlToTest(prisonNumber: String) = "alerts/case-notes/$prisonNumber"
}
