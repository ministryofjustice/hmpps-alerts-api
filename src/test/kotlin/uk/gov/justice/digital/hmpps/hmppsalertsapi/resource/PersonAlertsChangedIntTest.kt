package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CREATED
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PersonAlertsChangedIntTest : IntegrationTestBase() {

  @Test
  fun `create alert publishes a person alerts changed event`() {
    val prisonNumber = givenPrisonerExists("C1234PA")
    webTestClient.createAlert(
      prisonNumber = prisonNumber,
      request = CreateAlert(
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
    verifyPersonAlertsChanged(prisonNumber)
  }

  @Test
  fun `update alert publishes a person alerts changed event`() {
    val prisonNumber = givenPrisonerExists("U1234PA")
    val alert = givenAnAlert(alert(prisonNumber))

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

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
    }
    verifyPersonAlertsChanged(prisonNumber)
  }

  @Test
  fun `update alert does not publish if no changes`() {
    val prisonNumber = givenPrisonerExists("U1234NP")
    val alert = givenAnAlert(alert(prisonNumber))

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

    assertThat(hmppsEventsQueue.countAllMessagesOnQueue()).isEqualTo(0)
  }

  @Test
  fun `delete alert publishes a person alerts changed event`() {
    val prisonNumber = givenPrisonerExists("D1234PA")
    val alert = givenAnAlert(alert(prisonNumber))
    webTestClient.deleteAlert(alertUuid = alert.alertUuid)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_DELETED.eventType)
    }
    verifyPersonAlertsChanged(prisonNumber)
  }

  @Test
  fun `resync of alerts publishes a person alerts changed event`() {
    val prisonNumber = "R1234PA"
    webTestClient.resyncAlert(
      prisonNumber,
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
    verifyPersonAlertsChanged(prisonNumber)
  }

  @Test
  fun `bulk alert publishes a person alerts changed event for each prisoner number`() {
    val prisonerNumbers = arrayOf("A1234BC", "B2345CD", "C3456DE")
    givenPrisonersExist(*prisonerNumbers)

    val request = BulkCreateAlerts(
      prisonNumbers = prisonerNumbers.toList(),
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

  private fun WebTestClient.createAlert(
    source: Source = DPS,
    request: CreateAlert,
    prisonNumber: String = PRISON_NUMBER,
  ) = post().uri("/prisoners/$prisonNumber/alerts")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
    .headers(setAlertRequestContext(source = source))
    .exchange().successResponse<Alert>(CREATED)

  private fun WebTestClient.updateAlert(
    alertUuid: UUID,
    source: Source = DPS,
    request: UpdateAlert,
  ) = put().uri("/alerts/$alertUuid")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
    .headers(setAlertRequestContext(source = source))
    .bodyValue(request)
    .exchange().successResponse<Alert>()

  private fun WebTestClient.deleteAlert(
    alertUuid: UUID,
    source: Source = DPS,
  ) = delete().uri("/alerts/$alertUuid")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
    .headers(setAlertRequestContext(source = source))
    .exchange()
    .expectStatus().isNoContent
    .expectBody().isEmpty

  private fun WebTestClient.resyncAlert(prisonNumber: String, request: Collection<ResyncAlert>) =
    post().uri("/resync/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .exchange().successResponse<List<ResyncedAlert>>(CREATED)

  private fun WebTestClient.bulkCreateAlert(request: BulkCreateAlerts) =
    post().uri("/bulk-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange().successResponse<BulkAlert>(CREATED)
}
