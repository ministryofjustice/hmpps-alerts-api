package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictimSummary
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertEntity

class UpdateAlertIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  var uuid: UUID? = null

  @BeforeEach
  fun setup() {
    uuid = UUID.randomUUID()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.put()
      .uri("/alerts/$uuid")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.put()
      .uri("/alerts/$uuid")
      .bodyValue(updateAlertRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.put()
      .uri("/alerts/$uuid")
      .bodyValue(updateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.put()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.put()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage)
        .isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.put()
      .uri("/alerts/$uuid")
      .bodyValue(updateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.put()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage)
        .isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertsController.updateAlert(java.util.UUID,uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 alert not found`() {
    val response = webTestClient.put()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .bodyValue(updateAlertRequest())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Alert not found: Could not find alert with ID $uuid")
      assertThat(developerMessage)
        .isEqualTo("Could not find alert with ID $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `alert updated via DPS`() {
    val alert = createAlert()
    val request = updateAlertRequest()
    val updatedAlert = webTestClient.updateAlert(alert.alertUuid, source = DPS, request = request)
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(alertEntity.alertCode.code)!!
    val lastModifiedAuditEvent = alertEntity.lastModifiedAuditEvent()!!

    with(request) {
      assertThat(updatedAlert).isEqualTo(
        Alert(
          alert.alertUuid,
          alert.prisonNumber,
          alertCodeVictimSummary(),
          description,
          authorisedBy,
          activeFrom!!,
          activeTo,
          true,
          updatedAlert.comments,
          alert.createdAt,
          TEST_USER,
          TEST_USER_NAME,
          lastModifiedAuditEvent.actionedAt.withNano(0),
          TEST_USER,
          TEST_USER_NAME,
          lastModifiedAuditEvent.actionedAt.withNano(0),
          TEST_USER,
          TEST_USER_NAME,
        ),
      )
      with(updatedAlert.comments.single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt.withNano(0))
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }

      assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
        AlertEntity(
          alertId = alertEntity.alertId,
          alertUuid = alertEntity.alertUuid,
          alertCode = alertCode,
          prisonNumber = alertEntity.prisonNumber,
          description = description,
          authorisedBy = authorisedBy,
          activeFrom = activeFrom!!,
          activeTo = activeTo,
          createdAt = alertEntity.createdAt,
        ).apply { lastModifiedAt = lastModifiedAuditEvent.actionedAt },
      )
      with(lastModifiedAuditEvent) {
        assertThat(action).isEqualTo(AuditEventAction.UPDATED)
        assertThat(description).isEqualTo(
          """Updated alert description from '${alert.description}' to '${request.description}'
Updated authorised by from '${alert.authorisedBy}' to '$authorisedBy'
Updated active from from '${alert.activeFrom}' to '$activeFrom'
Updated active to from '${alert.activeTo}' to '$activeTo'
Comment '$appendComment' was added""",
        )
        assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
        assertThat(actionedBy).isEqualTo(TEST_USER)
        assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
        assertThat(source).isEqualTo(DPS)
        assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
        assertThat(descriptionUpdated).isTrue()
        assertThat(authorisedByUpdated).isTrue()
        assertThat(activeFromUpdated).isTrue()
        assertThat(activeToUpdated).isTrue()
        assertThat(commentAppended).isTrue()
      }
      with(alertEntity.comments().single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt)
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }
    }
  }

  @Test
  fun `alert updated without changing activeTo via DPS`() {
    val alert = createAlert()
    val request = updateAlertRequest(activeTo = alert.activeTo)
    val updatedAlert = webTestClient.updateAlert(alert.alertUuid, source = DPS, request = request)
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(alertEntity.alertCode.code)!!
    val lastModifiedAuditEvent = alertEntity.lastModifiedAuditEvent()!!

    with(request) {
      assertThat(updatedAlert).isEqualTo(
        Alert(
          alert.alertUuid,
          alert.prisonNumber,
          alertCodeVictimSummary(),
          description,
          authorisedBy,
          activeFrom!!,
          activeTo,
          true,
          updatedAlert.comments,
          alert.createdAt,
          TEST_USER,
          TEST_USER_NAME,
          lastModifiedAuditEvent.actionedAt.withNano(0),
          TEST_USER,
          TEST_USER_NAME,
          null,
          null,
          null,
        ),
      )

      assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
        AlertEntity(
          alertId = alertEntity.alertId,
          alertUuid = alertEntity.alertUuid,
          alertCode = alertCode,
          prisonNumber = alertEntity.prisonNumber,
          description = description,
          authorisedBy = authorisedBy,
          activeFrom = activeFrom!!,
          activeTo = activeTo,
          createdAt = alertEntity.createdAt,
        ).apply { lastModifiedAt = lastModifiedAuditEvent.actionedAt },
      )
      with(lastModifiedAuditEvent) {
        assertThat(action).isEqualTo(AuditEventAction.UPDATED)
        assertThat(description).isEqualTo(
          """Updated alert description from '${alert.description}' to '${request.description}'
Updated authorised by from '${alert.authorisedBy}' to '$authorisedBy'
Updated active from from '${alert.activeFrom}' to '$activeFrom'
Comment '$appendComment' was added""",
        )
        assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
        assertThat(actionedBy).isEqualTo(TEST_USER)
        assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
        assertThat(source).isEqualTo(DPS)
        assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
        assertThat(descriptionUpdated).isTrue()
        assertThat(authorisedByUpdated).isTrue()
        assertThat(activeFromUpdated).isTrue()
        assertThat(activeToUpdated).isFalse()
        assertThat(commentAppended).isTrue()
      }
    }
  }

  @Test
  fun `alert updated via NOMIS`() {
    val alert = createAlert()
    val request = updateAlertRequest()
    val updatedAlert = webTestClient.updateAlert(alert.alertUuid, source = NOMIS, request = request)
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(alertEntity.alertCode.code)!!
    val lastModifiedAuditEvent = alertEntity.lastModifiedAuditEvent()!!

    with(request) {
      assertThat(updatedAlert).isEqualTo(
        Alert(
          alert.alertUuid,
          alert.prisonNumber,
          alertCodeVictimSummary(),
          description,
          authorisedBy,
          activeFrom!!,
          activeTo,
          true,
          updatedAlert.comments,
          alert.createdAt,
          TEST_USER,
          TEST_USER_NAME,
          lastModifiedAuditEvent.actionedAt.withNano(0),
          TEST_USER,
          TEST_USER_NAME,
          lastModifiedAuditEvent.actionedAt.withNano(0),
          TEST_USER,
          TEST_USER_NAME,
        ),
      )
      with(updatedAlert.comments.single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt.withNano(0))
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }

      assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
        AlertEntity(
          alertId = alertEntity.alertId,
          alertUuid = alertEntity.alertUuid,
          alertCode = alertCode,
          prisonNumber = alertEntity.prisonNumber,
          description = description,
          authorisedBy = authorisedBy,
          activeFrom = activeFrom!!,
          activeTo = activeTo,
          createdAt = alertEntity.createdAt,
        ).apply { lastModifiedAt = lastModifiedAuditEvent.actionedAt },
      )
      with(lastModifiedAuditEvent) {
        assertThat(action).isEqualTo(AuditEventAction.UPDATED)
        assertThat(description).isEqualTo(
          """Updated alert description from '${alert.description}' to '${request.description}'
Updated authorised by from '${alert.authorisedBy}' to '$authorisedBy'
Updated active from from '${alert.activeFrom}' to '$activeFrom'
Updated active to from '${alert.activeTo}' to '$activeTo'
Comment '$appendComment' was added""",
        )
        assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
        assertThat(actionedBy).isEqualTo(TEST_USER)
        assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
        assertThat(source).isEqualTo(NOMIS)
        assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
      }
      with(alertEntity.comments().single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt)
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }
    }
  }

  @Test
  fun `add comment to alert`() {
    val alert = createAlert()
    val request = UpdateAlert(appendComment = "Additional comment")
    val updatedAlert = webTestClient.updateAlert(alert.alertUuid, request = request)
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val lastModifiedAuditEvent = alertEntity.lastModifiedAuditEvent()!!

    with(request) {
      with(updatedAlert.comments.single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt.withNano(0))
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }
      assertThat(alertEntity.lastModifiedAt).isEqualTo(lastModifiedAuditEvent.actionedAt)
      with(alertEntity.comments().single()) {
        assertThat(comment).isEqualTo(appendComment)
        assertThat(createdAt).isEqualTo(lastModifiedAuditEvent.actionedAt)
        assertThat(createdBy).isEqualTo(TEST_USER)
        assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
      }
      with(lastModifiedAuditEvent) {
        assertThat(action).isEqualTo(AuditEventAction.UPDATED)
        assertThat(description).isEqualTo("Comment '$appendComment' was added")
        assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
        assertThat(actionedBy).isEqualTo(TEST_USER)
        assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
        assertThat(source).isEqualTo(DPS)
        assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
      }
    }
  }

  @Test
  fun `should populate updated by display name using Username header when source is NOMIS`() {
    val alert = createAlert()

    val updatedAlert = webTestClient.put()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = NOMIS, username = NOMIS_SYS_USER))
      .bodyValue(updateAlertRequest())
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!

    with(updatedAlert) {
      assertThat(lastModifiedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(lastModifiedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should populate updated by username and display name as 'NOMIS' when source is NOMIS and no username is supplied`() {
    val alert = createAlert()

    val updatedAlert = webTestClient.put()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .header(SOURCE, NOMIS.name)
      .bodyValue(updateAlertRequest())
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!

    with(updatedAlert) {
      assertThat(lastModifiedBy).isEqualTo("NOMIS")
      assertThat(lastModifiedByDisplayName).isEqualTo("Nomis")
    }

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo("NOMIS")
      assertThat(actionedByDisplayName).isEqualTo("Nomis")
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should publish alert updated event with DPS source`() {
    val alert = createAlert()

    webTestClient.updateAlert(alert.alertUuid, DPS, updateAlertRequest())

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
    val updateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(updateAlertEvent.additionalInformation.identifier())
    assertThat(updateAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_UPDATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          alert.prisonNumber,
          alert.alertCode.code,
          DPS,
        ),
        1,
        ALERT_UPDATED.description,
        updateAlertEvent.occurredAt,
      ),
    )
    assertThat(
      updateAlertEvent.occurredAt.toLocalDateTime(),
    ).isCloseTo(alertRepository.findByAlertUuid(alert.alertUuid)!!.lastModifiedAt, within(1, ChronoUnit.MICROS))
  }

  @Test
  fun `should publish alert updated event with NOMIS source`() {
    val alert = createAlert()

    webTestClient.updateAlert(alert.alertUuid, NOMIS, updateAlertRequest())

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
    val updateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(updateAlertEvent.additionalInformation.identifier())
    assertThat(updateAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_UPDATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          alert.prisonNumber,
          alert.alertCode.code,
          NOMIS,
        ),
        1,
        ALERT_UPDATED.description,
        updateAlertEvent.occurredAt,
      ),
    )
    assertThat(
      updateAlertEvent.occurredAt.toLocalDateTime(),
    ).isCloseTo(alertRepository.findByAlertUuid(alert.alertUuid)!!.lastModifiedAt, within(1, ChronoUnit.MICROS))
  }

  private fun createAlertRequest(
    alertCode: String = ALERT_CODE_VICTIM,
  ) =
    CreateAlert(
      alertCode = alertCode,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = null,
    )

  private fun createAlert(prisonNumber: String = PRISON_NUMBER) =
    webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!

  private fun updateAlertRequest(
    comment: String = "Another update alert",
    activeTo: LocalDate? = LocalDate.now().plusMonths(10),
  ) =
    UpdateAlert(
      description = "another new description",
      authorisedBy = "C Cauthorizer",
      activeFrom = LocalDate.now().minusMonths(2),
      activeTo = activeTo,
      appendComment = comment,
    )

  private fun WebTestClient.updateAlert(
    alertUuid: UUID,
    source: Source = DPS,
    request: UpdateAlert,
  ) =
    put()
      .uri("/alerts/$alertUuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = source))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!
}
