package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_READY_FOR_WORK
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_SOCIAL_CARE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_MEDICAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_OTHER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_SOCIAL_CARE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class PrisonerAlertsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `empty response if no alerts found for prison number`() {
    val response = webTestClient.getPrisonerAlerts(PRISON_NUMBER_NOT_FOUND)
    assertThat(response.content).isEmpty()
    assertThat(response.empty).isTrue()
  }

  @Test
  fun `retrieve all alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber)
    assertThat(response).isEqualTo(
      AlertsPage(
        totalElements = 5,
        totalPages = 1,
        first = true,
        last = true,
        size = 2147483647,
        content = response.content,
        number = 0,
        sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
        numberOfElements = 5,
        pageable = AlertsPagePageable(
          offset = 0,
          sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
          paged = true,
          unpaged = false,
          pageNumber = 0,
          pageSize = 2147483647,
        ),
        empty = false,
      ),
    )
    with(response.content) {
      assertThat(this).hasSize(5)
      assertAllForPrisonNumber(prisonNumber)
      assertContainsActiveAndInactive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve pages of all alerts for prison number`() {
    val prisonNumber = setUp()
    val firstPage = webTestClient.getPrisonerAlerts(prisonNumber, page = 0, size = 3)
    val lastPage = webTestClient.getPrisonerAlerts(prisonNumber, page = 1, size = 3)
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
        pageable = AlertsPagePageable(
          offset = 0,
          sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
          paged = true,
          unpaged = false,
          pageNumber = 0,
          pageSize = 3,
        ),
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
        pageable = AlertsPagePageable(
          offset = 3,
          sort = AlertsPageSort(empty = false, unsorted = false, sorted = true),
          paged = true,
          unpaged = false,
          pageNumber = 1,
          pageSize = 3,
        ),
        empty = false,
      ),
    )
    with(lastPage.content) {
      assertThat(this).hasSize(2)
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all active alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, isActive = true)
    with(response.content) {
      assertThat(this).hasSize(4)
      assertAllForPrisonNumber(prisonNumber)
      assertContainsOnlyActive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all inactive alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, isActive = false)
    with(response.content) {
      assertThat(this).hasSize(1)
      assertAllForPrisonNumber(prisonNumber)
      assertContainsOnlyInactive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all 'M' - 'Medical' type alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, alertType = ALERT_TYPE_CODE_MEDICAL)
    with(response.content) {
      assertThat(this).hasSize(2)
      assertAllForPrisonNumber(prisonNumber)
      assertAllOfAlertType(ALERT_TYPE_CODE_MEDICAL)
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all 'A' - 'Social Care' (all active), 'M' - 'Medical' (all inactive) and 'O' - 'Other' (alert deleted) type alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(
      prisonNumber,
      alertType = listOf(ALERT_TYPE_SOCIAL_CARE, ALERT_TYPE_CODE_MEDICAL, ALERT_TYPE_CODE_OTHER).joinToString(","),
    )
    with(response.content) {
      assertThat(this).hasSize(4)
      assertAllForPrisonNumber(prisonNumber)
      assertAllOfAlertType(ALERT_TYPE_SOCIAL_CARE, ALERT_TYPE_CODE_MEDICAL)
      assertContainsActiveAndInactive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all 'ADSC' - 'Adult Social Care' code alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, alertCode = ALERT_CODE_SOCIAL_CARE)
    with(response.content) {
      assertThat(this).hasSize(1)
      assertAllForPrisonNumber(prisonNumber)
      assertAllOfAlertCode(ALERT_CODE_SOCIAL_CARE)
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all 'ADSC' - 'Adult Social Care' (active), 'URS' - 'Refusing to shield' (active, inactive code) and 'ORFW' - 'Ready For Work' (alert deleted) code alerts for prison number`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(
      prisonNumber,
      alertCode = listOf(
        ALERT_CODE_SOCIAL_CARE,
        ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD,
        ALERT_CODE_READY_FOR_WORK,
      ).joinToString(","),
    )
    with(response.content) {
      assertThat(this).hasSize(2)
      assertAllForPrisonNumber(prisonNumber)
      assertAllOfAlertCode(ALERT_CODE_SOCIAL_CARE, ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertContainsOnlyActive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all alerts for prison number active from on or after today`() {
    val activeFromStart = LocalDate.now()
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, activeFromStart = activeFromStart)
    with(response.content) {
      assertThat(this).hasSize(3)
      assertAllForPrisonNumber(prisonNumber)
      assertActiveFromOnOrAfter(activeFromStart)
      assertContainsOnlyActive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all alerts for prison number active from up to or before today`() {
    val activeFromStart = LocalDate.now()
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, activeFromEnd = activeFromStart)
    with(response.content) {
      assertThat(this).hasSize(4)
      assertAllForPrisonNumber(prisonNumber)
      assertActiveFromOnOrBefore(activeFromStart)
      assertContainsActiveAndInactive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all alerts for prison number with 'OciAL CaRE' in the description`() {
    val search = "OciAL CaRE"
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, search = search)
    with(response.content) {
      assertThat(this).hasSize(2)
      assertAllForPrisonNumber(prisonNumber)
      assertDescriptionContainsCaseInsensitive(search)
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `retrieve all alerts for prison number with 'PproVe' in the authorised by`() {
    val search = "PproVe"
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, search = search)
    with(response.content) {
      assertThat(this).hasSize(2)
      assertAllForPrisonNumber(prisonNumber)
      assertAuthorisedByContainsCaseInsensitive(search)
      assertContainsOnlyActive()
      assertOrderedByActiveFromDesc()
    }
  }

  @Test
  fun `sort all alerts for prison number by active from in ascending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("activeFrom,asc"))
    response.content.assertOrderedByActiveFromAsc()
  }

  @Test
  fun `sort all alerts for prison number by active to in ascending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("activeTo,asc"))
    response.content.assertOrderedByActiveToAsc()
  }

  @Test
  fun `sort all alerts for prison number by active to in descending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("activeTo,desc"))
    response.content.assertOrderedByActiveToDesc()
  }

  @Test
  fun `sort all alerts for prison number by created at in ascending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("createdAt,asc"))
    response.content.assertOrderedByCreatedAtAsc()
  }

  @Test
  fun `sort all alerts for prison number by created at in descending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("createdAt,desc"))
    response.content.assertOrderedByCreatedAtDesc()
  }

  @Test
  fun `sort all alerts for prison number by last modified at in ascending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("lastModifiedAt,asc"))
    response.content.assertOrderedByLastModifiedAtAsc()
  }

  @Test
  fun `sort all alerts for prison number by last modified at in descending order`() {
    val prisonNumber = setUp()
    val response = webTestClient.getPrisonerAlerts(prisonNumber, sort = arrayOf("lastModifiedAt,desc"))
    response.content.assertOrderedByLastModifiedAtDesc()
  }

  private fun Collection<Alert>.assertAllForPrisonNumber(prisonNumber: String) =
    assertThat(all { it.prisonNumber == prisonNumber }).isTrue

  private fun Collection<Alert>.assertAllOfAlertType(vararg alertType: String) =
    assertThat(all { alertType.contains(it.alertCode.alertTypeCode) }).isTrue

  private fun Collection<Alert>.assertAllOfAlertCode(vararg alertCode: String) =
    assertThat(all { alertCode.contains(it.alertCode.code) }).isTrue

  private fun Collection<Alert>.assertDescriptionContainsCaseInsensitive(search: String) =
    assertThat(all { it.description!!.contains(search, ignoreCase = true) }).isTrue

  private fun Collection<Alert>.assertAuthorisedByContainsCaseInsensitive(search: String) =
    assertThat(all { it.authorisedBy!!.contains(search, ignoreCase = true) }).isTrue

  private fun Collection<Alert>.assertActiveFromOnOrAfter(activeFrom: LocalDate) =
    assertThat(all { it.activeFrom.onOrAfter(activeFrom) }).isTrue

  private fun Collection<Alert>.assertActiveFromOnOrBefore(activeFrom: LocalDate) =
    assertThat(all { it.activeFrom.onOrBefore(activeFrom) }).isTrue

  private fun Collection<Alert>.assertContainsActiveAndInactive() =
    with(this) {
      assertThat(any { it.isActive }).isTrue
      assertThat(any { !it.isActive }).isTrue
    }

  private fun Collection<Alert>.assertContainsOnlyActive() =
    assertThat(all { it.isActive }).isTrue

  private fun Collection<Alert>.assertContainsOnlyInactive() =
    assertThat(none { it.isActive }).isTrue

  private fun List<Alert>.assertOrderedByActiveFromAsc() =
    assertThat(this).isSortedAccordingTo(compareBy { it.activeFrom })

  private fun List<Alert>.assertOrderedByActiveFromDesc() =
    assertThat(this).isSortedAccordingTo(compareByDescending { it.activeFrom })

  private fun List<Alert>.assertOrderedByActiveToAsc() =
    assertThat(this).isSortedAccordingTo(compareBy(nullsLast()) { it.activeTo })

  private fun List<Alert>.assertOrderedByActiveToDesc() =
    assertThat(this).isSortedAccordingTo(compareByDescending(nullsLast()) { it.activeTo })

  private fun List<Alert>.assertOrderedByCreatedAtAsc() =
    assertThat(this).isSortedAccordingTo(compareBy(nullsLast()) { it.createdAt })

  private fun List<Alert>.assertOrderedByCreatedAtDesc() =
    assertThat(this).isSortedAccordingTo(compareByDescending(nullsLast()) { it.createdAt })

  private fun List<Alert>.assertOrderedByLastModifiedAtAsc() =
    assertThat(this).isSortedAccordingTo(compareBy(nullsLast()) { it.lastModifiedAt })

  private fun List<Alert>.assertOrderedByLastModifiedAtDesc() =
    assertThat(this).isSortedAccordingTo(compareByDescending(nullsLast()) { it.lastModifiedAt })

  private fun WebTestClient.getPrisonerAlerts(
    prisonNumber: String,
    isActive: Boolean? = null,
    alertType: String? = null,
    alertCode: String? = null,
    activeFromStart: LocalDate? = null,
    activeFromEnd: LocalDate? = null,
    search: String? = null,
    page: Int? = null,
    size: Int? = null,
    sort: Array<String>? = null,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prisoners/$prisonNumber/alerts")
          .queryParamIfPresent("isActive", Optional.ofNullable(isActive))
          .queryParamIfPresent("alertType", Optional.ofNullable(alertType))
          .queryParamIfPresent("alertCode", Optional.ofNullable(alertCode))
          .queryParamIfPresent("activeFromStart", Optional.ofNullable(activeFromStart))
          .queryParamIfPresent("activeFromEnd", Optional.ofNullable(activeFromEnd))
          .queryParamIfPresent("search", Optional.ofNullable(search))
          .queryParamIfPresent("page", Optional.ofNullable(page))
          .queryParamIfPresent("size", Optional.ofNullable(size))
          .also {
            sort?.forEach {
              builder.queryParam("sort", it)
            }
          }
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().successResponse<AlertsPage>()

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

  private fun setUp(): String {
    val prisonNumber = prisonNumber()
    val alerts = mutableListOf(
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("ADSC"),
        description = "Active alert type 'A' - 'Social Care' code 'ADSC' - 'Adult Social Care' alert for prison number 'A1234AA' active from yesterday with no active to date. Alert code is active. Created yesterday and not modified since",
        activeFrom = LocalDate.now().minusDays(1),
        createdAt = LocalDateTime.now().minusDays(1),
        authorisedBy = "A. Approver",
      ),
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("AS"),
        description = "Active alert type 'A' - 'Social Care' code 'AS' - 'Social Care' alert for prison number 'A1234AA' active from today with no active to date. Alert code is active. Created two days ago and modified yesterday and today",
        activeFrom = LocalDate.now(),
        createdAt = LocalDateTime.now().minusDays(2),
        authorisedBy = "External Provider",
      ),
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("URS"),
        description = "Active alert type 'U' - 'COVID unit management' code 'URS' - 'Refusing to shield' alert for prison number 'A1234AA' active from today with no active to date. Alert code is inactive. Created today and modified shortly after",
        activeFrom = LocalDate.now(),
        createdAt = LocalDateTime.now().minusHours(1),
        authorisedBy = null,
      ),
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("MAS"),
        description = "Inactive alert type 'M' - 'Medical' code 'MAS' - 'Asthmatic' alert for prison number 'A1234AA' active from tomorrow with no active to date. Alert code is active. Created three days ago and not modified since",
        activeFrom = LocalDate.now().plusDays(1),
        createdAt = LocalDateTime.now().minusDays(3),
        authorisedBy = "B. Approver",
      ),
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("MEP"),
        description = "'Inactive alert type 'M' - 'Medical' code 'MEP' - 'Epileptic' alert for prison number 'A1234AA' active from yesterday to today. Alert code is active. Created four days ago and modified yesterday",
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = LocalDate.now(),
        createdAt = LocalDateTime.now().minusDays(4),
        authorisedBy = null,
      ),
      alert(
        prisonNumber,
        alertCode = givenExistingAlertCode("ORFW"),
        description = "Deleted active alert type 'O' - 'Other' code 'ORFW' - 'Ready For Work' alert for prison number 'A1234AA' which would have been active from today with no active to date. Alert code is active. Created today and not modified since",
        activeFrom = LocalDate.now(),
        createdAt = LocalDateTime.now(),
        deletedAt = LocalDateTime.now(),
        authorisedBy = "C. Approver",
      ),
      alert(
        prisonNumber(),
        alertCode = givenExistingAlertCode("ADSC"),
        description = "Active alert type 'A' - 'Social Care' code 'ADSC' - 'Adult Social Care' alert for prison number 'B2345BB' active from three days ago with active to from two days ago. Alert code is inactive. Created yesterday and not modified since",
        activeFrom = LocalDate.now().minusDays(3),
        activeTo = LocalDate.now().minusDays(2),
        createdAt = LocalDateTime.now().minusDays(3),
        authorisedBy = null,
      ),
    )
    alertRepository.saveAll(
      alerts.map {
        it.create(
          createdBy = "SYS",
          createdByDisplayName = "Sys",
          source = Source.DPS,
          activeCaseLoadId = null,
        )
      },
    )
    return prisonNumber
  }
}
