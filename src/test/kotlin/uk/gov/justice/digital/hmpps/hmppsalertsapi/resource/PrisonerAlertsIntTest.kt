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
import java.util.Optional
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
    assertThat(response.empty).isTrue()
  }

  @Test
  @Sql("classpath:test_data/prisoner-alerts-paginate-filter-sort.sql")
  fun `retrieve all alerts for prison number`() {
    val response = webTestClient.getPrisonerAlerts(PRISON_NUMBER)
    assertThat(response).isEqualTo(
      AlertsPage(
        totalElements = 5,
        totalPages = 1,
        first = true,
        last = true,
        size = 10,
        content = response.content,
        number = 0,
        sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
        numberOfElements = 5,
        pageable = AlertsPagePageable(offset = 0, sort = AlertsPageSort(empty = false, unsorted = false, sorted = true), paged = true, unpaged = false, pageNumber = 0, pageSize = 10),
        empty = false,
      ),
    )
    with(response.content) {
      assertThat(this).hasSize(5)
      assertAllForPrisonNumber(PRISON_NUMBER)
      assertContainsActiveAndInactiveAlertCodes()
      assertContainsActiveAndInactive()
      assertDoesNotContainDeletedAlert()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  @Sql("classpath:test_data/prisoner-alerts-paginate-filter-sort.sql")
  fun `retrieve pages of alerts for prison number`() {
    val firstPage = webTestClient.getPrisonerAlerts(PRISON_NUMBER, page = 0, size = 3)
    val lastPage = webTestClient.getPrisonerAlerts(PRISON_NUMBER, page = 1, size = 3)
    assertThat(firstPage).isEqualTo(
      AlertsPage(
        totalElements = 5,
        totalPages = 2,
        first = true,
        last = false,
        size = 3,
        content = firstPage.content,
        number = 0,
        sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
        numberOfElements = 3,
        pageable = AlertsPagePageable(offset = 0, sort = AlertsPageSort(empty = false, unsorted = false, sorted = true), paged = true, unpaged = false, pageNumber = 0, pageSize = 3),
        empty = false,
      ),
    )
    with(firstPage.content) {
      assertThat(this).hasSize(3)
      assertOrderedByActiveFromDesc()
    }
    assertThat(lastPage).isEqualTo(
      AlertsPage(
        totalElements = 5,
        totalPages = 2,
        first = false,
        last = true,
        size = 3,
        content = lastPage.content,
        number = 1,
        sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
        numberOfElements = 2,
        pageable = AlertsPagePageable(offset = 3, sort = AlertsPageSort(empty = false, unsorted = false, sorted = true), paged = true, unpaged = false, pageNumber = 1, pageSize = 3),
        empty = false,
      ),
    )
    with(lastPage.content) {
      assertThat(this).hasSize(2)
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
    page: Int? = null,
    size: Int? = null,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prisoner/$prisonNumber/alerts")
          .queryParamIfPresent("page", Optional.ofNullable(page))
          .queryParamIfPresent("size", Optional.ofNullable(size))
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertsPage::class.java)
      .returnResult().responseBody!!

  data class AlertsPage(
    val totalElements: Int,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val size: Int,
    val content: List<Alert>,
    val number: Int,
    val sort: AlertsPageSort,
    val numberOfElements: Int,
    val pageable: AlertsPagePageable,
    val empty: Boolean,
  )

  data class AlertsPagePageable(
    val offset: Int,
    val sort: AlertsPageSort,
    val paged: Boolean,
    val unpaged: Boolean,
    val pageNumber: Int,
    val pageSize: Int,
  )

  data class AlertsPageSort(
    val empty: Boolean,
    val unsorted: Boolean,
    val sorted: Boolean,
  )
}
