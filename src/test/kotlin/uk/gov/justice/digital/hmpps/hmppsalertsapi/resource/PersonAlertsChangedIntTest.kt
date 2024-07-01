package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.MergeAlertsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERTS_MERGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_HIDDEN_DISABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PersonAlertsChangedIntTest : IntegrationTestBase() {

  @Test
  fun `create alert publishes a person alerts changed event`() {
    webTestClient.createAlert(
      request = CreateAlert(
        prisonNumber = PRISON_NUMBER,
        alertCode = ALERT_CODE_VICTIM,
        description = "Alert description",
        authorisedBy = "C Smith",
        activeFrom = LocalDate.now().minusDays(3),
        activeTo = null,
      ),
    )

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
    }
    verifyPersonAlertsChanged(PRISON_NUMBER)
  }

  @Test
  fun `update alert publishes a person alerts changed event`() {
    val alert = createAlert()
    webTestClient.updateAlert(
      alertUuid = alert.alertUuid,
      request = UpdateAlert(
        description = "Updated description",
        authorisedBy = "U dated",
        activeFrom = LocalDate.now().minusDays(2),
        activeTo = LocalDate.now().plusDays(30),
        appendComment = null,
      ),
    )

    // Two of these messages are from the create
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
    }
    verifyPersonAlertsChanged(PRISON_NUMBER)
  }

  @Test
  fun `update alert does not publish if no changes`() {
    val alert = createAlert()
    webTestClient.updateAlert(
      alertUuid = alert.alertUuid,
      request = UpdateAlert(
        description = alert.description,
        authorisedBy = alert.authorisedBy,
        activeFrom = alert.activeFrom,
        activeTo = alert.activeTo,
        appendComment = null,
      ),
    )

    // These two messages are from the create
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
  }

  @Test
  fun `delete alert publishes a person alerts changed event`() {
    val alert = createAlert()
    webTestClient.deleteAlert(alertUuid = alert.alertUuid)

    // Two of these messages are from the create
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_DELETED.eventType)
    }
    verifyPersonAlertsChanged(PRISON_NUMBER)
  }

  @Test
  fun `resync of alerts publishes a person alerts changed event`() {
    webTestClient.resyncAlert(
      request = listOf(
        ResyncAlert(
          offenderBookId = 12345,
          alertSeq = 2,
          alertCode = ALERT_CODE_VICTIM,
          description = "Alert description",
          authorisedBy = "A. Nurse, An Agency",
          activeFrom = LocalDate.now().minusDays(2),
          activeTo = null,
          createdAt = LocalDateTime.now().minusDays(2).withNano(0),
          createdBy = "AB11DZ",
          createdByDisplayName = "C REATED",
          lastModifiedAt = null,
          lastModifiedBy = null,
          lastModifiedByDisplayName = null,
        ),
      ),
    )

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
    }
    verifyPersonAlertsChanged(PRISON_NUMBER)
  }

  @Test
  fun `merge alerts publishes a person alerts changed event for both prisoner numbers`() {
    val request = MergeAlerts(
      prisonNumberMergeFrom = "B2345BB",
      prisonNumberMergeTo = PRISON_NUMBER,
      newAlerts = listOf(
        MergeAlert(
          offenderBookId = 12345,
          alertSeq = 1,
          alertCode = ALERT_CODE_HIDDEN_DISABILITY,
          description = "Alert description",
          authorisedBy = "A. Nurse, An Agency",
          activeFrom = LocalDate.now().minusDays(1),
          activeTo = null,
        ),
      ),
      retainedAlertUuids = listOf(),
    )

    webTestClient.mergeAlerts(request = request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<MergeAlertsAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERTS_MERGED.eventType)
    }
    verifyPersonAlertsChanged(request.prisonNumberMergeFrom)
    verifyPersonAlertsChanged(request.prisonNumberMergeTo)
  }

  @Test
  fun `bulk alert publishes a person alerts changed event for each prisoner number`() {
    val prisonerNumbers = listOf("A1234BC", "B2345CD", "C3456DE")
    prisonerSearch.stubGetPrisoners(prisonerNumbers)

    val request = BulkCreateAlerts(
      prisonNumbers = prisonerNumbers,
      alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
      mode = ADD_MISSING,
      cleanupMode = KEEP_ALL,
    )

    webTestClient.bulkCreateAlert(request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 6 }
    val types = hmppsEventsQueue.receiveMessageTypeCounts(6)
    assertThat(types[PERSON_ALERTS_CHANGED.eventType]).isEqualTo(3)
    assertThat(types[ALERT_CREATED.eventType]).isEqualTo(3)
  }

  private fun verifyPersonAlertsChanged(prisonNumber: String) {
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
      assertThat(personReference.findNomsNumber()).isEqualTo(prisonNumber)
    }
  }

  private fun createAlert(): Alert {
    val alert = webTestClient.createAlert(
      request = CreateAlert(
        prisonNumber = PRISON_NUMBER,
        alertCode = ALERT_CODE_VICTIM,
        description = "Alert description",
        authorisedBy = "C Smith",
        activeFrom = LocalDate.now().minusDays(5),
        activeTo = null,
      ),
    )
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
    }
    verifyPersonAlertsChanged(PRISON_NUMBER)
    return alert
  }

  private fun WebTestClient.createAlert(
    source: Source = DPS,
    request: CreateAlert,
    prisonNumber: String = PRISON_NUMBER,
  ) = post().uri("/prisoners/$prisonNumber/alerts")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
    .headers(setAlertRequestContext(source = source))
    .exchange()
    .expectStatus().isCreated
    .expectBody(Alert::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.updateAlert(
    alertUuid: UUID,
    source: Source = DPS,
    request: UpdateAlert,
  ) = put().uri("/alerts/$alertUuid")
    .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
    .headers(setAlertRequestContext(source = source))
    .bodyValue(request)
    .exchange()
    .expectStatus().isOk
    .expectBody(Alert::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.deleteAlert(
    alertUuid: UUID,
    source: Source = DPS,
  ) = delete().uri("/alerts/$alertUuid")
    .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
    .headers(setAlertRequestContext(source = source))
    .exchange()
    .expectStatus().isNoContent
    .expectBody().isEmpty

  private fun WebTestClient.resyncAlert(request: Collection<ResyncAlert>) =
    post().uri("/resync/$PRISON_NUMBER/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .exchange()
      .expectStatus().isCreated
      .expectBodyList(ResyncedAlert::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.mergeAlerts(request: MergeAlerts) =
    post().uri("/merge-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .exchange()
      .expectStatus().isCreated
      .expectBody(MergedAlerts::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.bulkCreateAlert(request: BulkCreateAlerts) =
    post().uri("/bulk-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectStatus().isCreated
      .expectBody(BulkAlert::class.java)
      .returnResult().responseBody!!
}
