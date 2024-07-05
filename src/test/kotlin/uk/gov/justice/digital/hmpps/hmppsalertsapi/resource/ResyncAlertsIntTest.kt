package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import com.fasterxml.jackson.module.kotlin.treeToValue
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.ResyncAuditRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.ResyncAuditRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_POOR_COPER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class ResyncAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Autowired
  lateinit var resyncAuditRepository: ResyncAuditRepository

  @Test
  fun `when not authorised - 401 unauthorised`() {
    webTestClient.post()
      .uri("/resync/$PRISON_NUMBER/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `when no appropriate role - 403 forbidden`() {
    webTestClient.post()
      .uri("/resync/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf("UNKNOWN_ROLE")))
      .bodyValue(listOf(resyncAlert()))
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest(name = "{0} allowed")
  @ValueSource(strings = [ROLE_ALERTS_ADMIN, ROLE_NOMIS_ALERTS])
  fun `specified roles are allowed to call the endpoint`(role: String) {
    val alert = resyncAlert()
    webTestClient.resyncResponseSpec(role, listOf(alert)).expectStatus().isCreated
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = listOf(
      resyncAlert(),
      resyncAlert().copy(alertCode = "NOT_FOUND_1"),
      resyncAlert().copy(alertCode = "NOT_FOUND_2"),
      resyncAlert().copy(alertCode = "NOT_FOUND_1"),
    )

    val response = webTestClient.resyncResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code(s) NOT_FOUND_1, NOT_FOUND_2 not found")
      assertThat(developerMessage).isEqualTo("Alert code(s) NOT_FOUND_1, NOT_FOUND_2 not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `resync of updated alert should create audit event`() {
    val request = resyncAlert().copy(
      lastModifiedAt = LocalDateTime.now().minusDays(1),
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.resyncAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(lastModifiedAt?.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
        request.lastModifiedAt?.truncatedTo(
          ChronoUnit.SECONDS,
        ),
      )
      with(lastModifiedAuditEvent()!!) {
        assertThat(actionedAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(request.lastModifiedAt?.truncatedTo(ChronoUnit.SECONDS))
        assertThat(actionedBy).isEqualTo(request.lastModifiedBy)
        assertThat(actionedByDisplayName).isEqualTo(request.lastModifiedByDisplayName)
      }
    }
  }

  @Test
  fun `resync alert with inactive alert code`() {
    val resyncedAlert =
      webTestClient.resyncAlerts(request = listOf(resyncAlert().copy(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)))
        .single()

    with(alertRepository.findByAlertUuid(resyncedAlert.alertUuid)!!.alertCode) {
      assertThat(code).isEqualTo(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `resync alert with active to before active from`() {
    val migratedAlert = webTestClient.resyncAlerts(
      request = listOf(
        resyncAlert().copy(
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now().minusDays(1),
        ),
      ),
    ).single()

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!) {
      assertThat(activeTo).isBefore(activeFrom)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `Successful resync results in 201 created response and sending of domain events`() {
    val existingAlert = Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeRepository.findByCode(ALERT_CODE_VICTIM)!!,
      prisonNumber = PRISON_NUMBER,
      description = "Existing alert - to be deleted",
      authorisedBy = "J Smith",
      activeFrom = LocalDate.now().minusDays(60),
      activeTo = LocalDate.now().plusDays(5),
      createdAt = LocalDateTime.now().minusDays(60),
    ).also { it.lastModifiedAt = null }
    alertRepository.save(existingAlert)

    val alert = resyncAlert()
    val response = webTestClient.resyncAlerts(request = listOf(alert)).single()

    assertThat(response.offenderBookId).isEqualTo(alert.offenderBookId)
    assertThat(response.alertSeq).isEqualTo(alert.alertSeq)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }
    val typeCounts = hmppsEventsQueue.receiveMessageTypeCounts(3)
    assertThat(typeCounts[DomainEventType.ALERT_CREATED.eventType]).isEqualTo(1)
    assertThat(typeCounts[DomainEventType.ALERT_DELETED.eventType]).isEqualTo(1)
    assertThat(typeCounts[DomainEventType.PERSON_ALERTS_CHANGED.eventType]).isEqualTo(1)

    val deletedAlert = alertRepository.findByAlertUuid(existingAlert.alertUuid)
    assertThat(deletedAlert).isNull()
  }

  @Test
  fun `Successful resync copies audit history when matching existing alert`() {
    val originalAlert = alertWithAuditHistory()
    val originalAudit = originalAlert.auditEvents()
    assertThat(originalAudit.size).isEqualTo(2)

    val alert = resyncAlert().copy(
      alertCode = originalAlert.alertCode.code,
      activeFrom = originalAlert.activeFrom,
      activeTo = originalAlert.activeTo,
    )
    val response = webTestClient.resyncAlerts(request = listOf(alert)).single()

    assertThat(response.offenderBookId).isEqualTo(alert.offenderBookId)
    assertThat(response.alertSeq).isEqualTo(alert.alertSeq)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 5 }
    val typeCounts = hmppsEventsQueue.receiveMessageTypeCounts(5)
    assertThat(typeCounts[DomainEventType.ALERT_CREATED.eventType]).isEqualTo(2)
    assertThat(typeCounts[DomainEventType.ALERT_UPDATED.eventType]).isEqualTo(1)
    assertThat(typeCounts[DomainEventType.ALERT_DELETED.eventType]).isEqualTo(1)
    assertThat(typeCounts[DomainEventType.PERSON_ALERTS_CHANGED.eventType]).isEqualTo(1)

    // original alert has been deleted
    val deletedAlert = alertRepository.findByAlertUuid(originalAlert.alertUuid)
    assertThat(deletedAlert).isNull()

    // new alert created with original audit history
    val newAlert = checkNotNull(alertRepository.findByAlertUuid(response.alertUuid))
    val newAudit = newAlert.auditEvents()
    assertThat(newAudit.size).isEqualTo(originalAudit.size)
    assertAuditEventsEqual(newAudit[0], originalAudit[0])
    assertAuditEventsEqual(newAudit[1], originalAudit[1])
  }

  private fun assertAuditEventsEqual(newAudit: AuditEvent, originalAudit: AuditEvent) {
    assertThat(newAudit.action).isEqualTo(originalAudit.action)
    assertThat(newAudit.actionedAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
      originalAudit.actionedAt.truncatedTo(ChronoUnit.SECONDS),
    )
    assertThat(newAudit.actionedBy).isEqualTo(originalAudit.actionedBy)
    assertThat(newAudit.actionedByDisplayName).isEqualTo(originalAudit.actionedByDisplayName)
    assertThat(newAudit.source).isEqualTo(originalAudit.source)
  }

  @Test
  fun `Successful resync creates a resync audit record`() {
    val originalAlert = alertWithAuditHistory()
    val alert1 = resyncAlert().copy(
      alertCode = originalAlert.alertCode.code,
      activeFrom = originalAlert.activeFrom,
      activeTo = originalAlert.activeTo,
      createdAt = originalAlert.createdAt,
    )
    val alert2 = resyncAlert().copy(
      alertCode = ALERT_CODE_POOR_COPER,
    )

    val response = webTestClient.resyncAlerts(request = listOf(alert1, alert2))

    val resyncAudit = resyncAuditRepository.findResyncAuditsByPrisonNumber(PRISON_NUMBER).single()
    assertThat(objectMapper.treeToValue<ResyncAuditRequest>(resyncAudit.request))
      .isEqualTo(ResyncAuditRequest(PRISON_NUMBER, listOf(alert1, alert2)))
    assertThat(resyncAudit.alertsCreated).containsAll(response.map { it.alertUuid })
    assertThat(resyncAudit.alertsDeleted).contains(originalAlert.alertUuid)
  }

  @Test
  fun `Passing empty list to resync removes alerts and sends domain events`() {
    val existingAlert = Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeRepository.findByCode(ALERT_CODE_VICTIM)!!,
      prisonNumber = PRISON_NUMBER,
      description = "Existing alert - to be deleted",
      authorisedBy = "J Smith",
      activeFrom = LocalDate.now().minusDays(60),
      activeTo = LocalDate.now().plusDays(5),
      createdAt = LocalDateTime.now().minusDays(60),
    ).also { it.lastModifiedAt = null }
    alertRepository.save(existingAlert)

    webTestClient.resyncAlerts(request = emptyList())

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val typeCounts = hmppsEventsQueue.receiveMessageTypeCounts(2)
    assertThat(typeCounts[DomainEventType.PERSON_ALERTS_CHANGED.eventType]).isEqualTo(1)
    assertThat(typeCounts[DomainEventType.ALERT_DELETED.eventType]).isEqualTo(1)
    val deletedAlert = alertRepository.findByAlertUuid(existingAlert.alertUuid)
    assertThat(deletedAlert).isNull()
  }

  private fun alertWithAuditHistory() = alertRepository.save(
    Alert(
      alertUuid = UUID.randomUUID(),
      alertCode = alertCodeRepository.findByCode(ALERT_CODE_VICTIM)!!,
      prisonNumber = PRISON_NUMBER,
      description = "Existing alert - to be deleted",
      authorisedBy = "J Smith",
      activeFrom = LocalDate.now().minusDays(60),
      activeTo = LocalDate.now().plusDays(5),
      createdAt = LocalDateTime.now().minusDays(60),
    ).also {
      it.lastModifiedAt = LocalDateTime.now()
      it.create(
        createdBy = "J Smith",
        createdByDisplayName = "John Smith",
        source = Source.NOMIS,
        activeCaseLoadId = null,
      )
      it.update(
        description = "Updated for testing",
        authorisedBy = null,
        activeFrom = null,
        activeTo = null,
        appendComment = null,
        updatedBy = "A Jones",
        updatedByDisplayName = "Andrew Jones",
        source = Source.DPS,
        activeCaseLoadId = null,
      )
    },
  )

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(
    request: ResyncAlert,
    expectedUserMessage: String,
    displayName: String,
  ) {
    val response = webTestClient.resyncResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).startsWith("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(
        resyncAlert().copy(offenderBookId = 0),
        "Offender book id must be supplied and be > 0",
        "offender book id required",
      ),
      Arguments.of(
        resyncAlert().copy(alertSeq = 0),
        "Alert sequence must be supplied and be > 0",
        "alert sequence required",
      ),
      Arguments.of(
        resyncAlert().copy(alertCode = ""),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        resyncAlert().copy(alertCode = 'a'.toString().repeat(13)),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
      Arguments.of(
        resyncAlert().copy(description = 'a'.toString().repeat(4001)),
        "Description must be <= 4000 characters",
        "description greater than 4000 characters",
      ),
      Arguments.of(
        resyncAlert().copy(authorisedBy = 'a'.toString().repeat(41)),
        "Authorised by must be <= 40 characters",
        "authorised by greater than 40 characters",
      ),
      Arguments.of(
        resyncAlert().copy(createdBy = ""),
        "Created by must be supplied and be <= 32 characters",
        "created by required",
      ),
      Arguments.of(
        resyncAlert().copy(createdBy = 'a'.toString().repeat(33)),
        "Created by must be supplied and be <= 32 characters",
        "created by greater than 32 characters",
      ),
      Arguments.of(
        resyncAlert().copy(createdByDisplayName = ""),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name required",
      ),
      Arguments.of(
        resyncAlert().copy(createdByDisplayName = 'a'.toString().repeat(256)),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name greater than 255 characters",
      ),
      Arguments.of(
        resyncAlert().copy(
          lastModifiedAt = LocalDateTime.now(),
          lastModifiedBy = null,
          lastModifiedByDisplayName = "Up Dated",
        ),
        "Last modified by is required when last modified at is supplied",
        "last modified by required when last modified at is supplied",
      ),
      Arguments.of(
        resyncAlert().copy(lastModifiedBy = 'a'.toString().repeat(33)),
        "Last modified by must be <= 32 characters",
        "last modified by greater than 32 characters",
      ),
      Arguments.of(
        resyncAlert().copy(lastModifiedAt = LocalDateTime.now(), lastModifiedBy = "AB11DZ"),
        "Last modified by display name is required when last modified at is supplied",
        "last modified by display name required when last modified at is supplied",
      ),
      Arguments.of(
        resyncAlert().copy(lastModifiedByDisplayName = 'a'.toString().repeat(256)),
        "Last modified by display name must be <= 255 characters",
        "last modified by display name greater than 255 characters",
      ),
    )

    fun resyncAlert() = ResyncAlert(
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
    )
  }

  private fun WebTestClient.resyncResponseSpec(role: String = ROLE_NOMIS_ALERTS, request: Collection<ResyncAlert>) =
    post()
      .uri("/resync/$PRISON_NUMBER/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  fun WebTestClient.resyncAlerts(role: String = ROLE_NOMIS_ALERTS, request: Collection<ResyncAlert>) =
    resyncResponseSpec(role, request)
      .expectStatus().isCreated
      .expectBodyList(ResyncedAlert::class.java)
      .returnResult().responseBody!!
}
