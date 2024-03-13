package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import java.util.UUID

class PrisonerAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  private val deletedAlertUuid = UUID.fromString("84856971-0072-40a9-ba5d-e994b0a9754f")

  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/prisoner/$PRISON_NUMBER/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/prisoner/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.get()
      .uri("/prisoner/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `empty response if no alerts found for prison number`() {
    val response = webTestClient.getPrisonerAlerts(PRISON_NUMBER)
    assertThat(response.content).isEmpty()
  }

  @Test
  @Sql("classpath:test_data/prisoner-alerts-paginate-filter-sort.sql")
  fun `retrieve all alerts for prison number`() {
    val response = webTestClient.getPrisonerAlerts(PRISON_NUMBER)
    with(response.content) {
      assertThat(this).isNotEmpty
      assertAllForPrisonNumber(PRISON_NUMBER)
      assertContainsActiveAndInactiveAlertCodes()
      assertContainsActiveAndInactive()
      assertDoesNotContainDeletedAlert()
      assertOrderedByActiveFromDesc()
    }
  }

  private fun Collection<Alert>.assertAllForPrisonNumber(prisonNumber: String) =
    assertThat(all { it.prisonNumber == prisonNumber }).isTrue

  private fun Collection<Alert>.assertContainsActiveAndInactiveAlertCodes() =
    with(this) {
      assertThat(any { it.alertCode.isActive }).isTrue
      assertThat(any { !it.alertCode.isActive }).isTrue
    }

  private fun Collection<Alert>.assertDoesNotContainDeletedAlert() =
    alertRepository.findByAlertUuidIncludingSoftDelete(deletedAlertUuid)!!.also { deletedAlert ->
      assertThat(deletedAlert.prisonNumber == map { it.prisonNumber }.distinct().single()).isTrue
      assertThat(none { it.alertUuid == deletedAlert.alertUuid }).isTrue
    }

  private fun Collection<Alert>.assertContainsActiveAndInactive() =
    with(this) {
      assertThat(any { it.isActive }).isTrue
      assertThat(any { !it.isActive }).isTrue
    }

  private fun List<Alert>.assertOrderedByActiveFromDesc() =
    assertThat(this).isSortedAccordingTo(compareByDescending { it.activeFrom })

  private fun WebTestClient.getPrisonerAlerts(
    prisonNumber: String,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prisoner/$prisonNumber/alerts")
          // .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive))
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertsPage::class.java)
      .returnResult().responseBody!!

  data class AlertsPage(
    val content: List<Alert>,
    /*content: TPagedListItem[]
    pageable?: {
    sort: {
      empty: boolean
      sorted: boolean
      unsorted: boolean
    }
    offset: number
    pageSize: number
    pageNumber: number
    paged: boolean
    unpaged: boolean
  }
  totalPages: number
  last: boolean
  totalElements: number
  size: number
  number: number
  sort: {
    empty: boolean
    sorted: boolean
    unsorted: boolean
  }
  first: boolean
  numberOfElements: number
  empty: boolean
  )*/
  )
}
