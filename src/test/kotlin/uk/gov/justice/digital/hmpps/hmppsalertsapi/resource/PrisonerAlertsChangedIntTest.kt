package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.backfill.PrisonerAlertsChanged
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalTime

class PrisonerAlertsChangedIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest()).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get().uri {
      it.path(urlToTest())
      it.queryParam("from", now())
      it.queryParam("to", now())
      it.build()
    }.headers(setAuthorisation())
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `get alerts for contacts`() {
    val prisonNumbers = listOf(prisonNumber(), prisonNumber(), prisonNumber())
    val from = now().minusDays(1)
    val codes = alertCodeRepository.findAll().shuffled().take(3)
    givenAlert(
      alert(
        prisonNumbers[0],
        codes[0],
        activeFrom = from.minusDays(2),
        createdAt = from.minusDays(1).atStartOfDay(),
      ),
    )
    givenAlert(
      alert(
        prisonNumbers[1],
        codes[1],
        activeFrom = from,
        createdAt = from.atTime(LocalTime.now()),
      ),
    )
    givenAlert(
      alert(
        prisonNumbers[2],
        codes[2],
        activeFrom = from.minusDays(3),
        createdAt = from.minusDays(3).atTime(LocalTime.now()),
        activeTo = from.plusDays(3),
      ).update(
        "Updated description",
        null,
        from.minusDays(3),
        from,
        from.atTime(LocalTime.now()),
        "ANO123",
        "A N Other",
        Source.DPS,
        null,
      ),
    )

    val res = getChangesBetween(from, from.plusDays(1))
    assertThat(res.personIdentifiers).containsExactlyInAnyOrder(prisonNumbers[1], prisonNumbers[2])
  }

  private fun getChangesBetween(from: LocalDate, to: LocalDate): PrisonerAlertsChanged = webTestClient.get()
    .uri {
      it.path(urlToTest())
      it.queryParam("from", from)
      it.queryParam("to", to)
      it.build()
    }
    .headers(setAuthorisation(roles = listOf(ROLE_CASE_NOTES)))
    .exchange()
    .expectStatus().isOk
    .expectBody<PrisonerAlertsChanged>()
    .returnResult().responseBody!!

  fun urlToTest() = "alerts/case-notes/changed"
}
