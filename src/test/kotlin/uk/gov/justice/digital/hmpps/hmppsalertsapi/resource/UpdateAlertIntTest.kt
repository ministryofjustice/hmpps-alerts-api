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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.ALERTS_SERVICE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
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
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertModel

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
  fun `alert updated`() {
    val alert = createAlert()
    val response = webTestClient.updateAlert(alert.alertUuid, request = updateAlertRequest())
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(alertEntity.alertCode.code)!!

    with(response!!) {
      assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
        AlertModel(
          alertId = 1,
          alertUuid = alert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
          description = description,
          authorisedBy = authorisedBy,
          activeFrom = activeFrom,
          activeTo = activeTo,
          createdAt = alertEntity.createdAt,
        ),
      )
    }

    with(alertEntity.auditEvents()[0]) {
      assertThat(auditEventId).isEqualTo(2)
      assertThat(action).isEqualTo(AuditEventAction.UPDATED)
      assertThat(description).isEqualTo(
        """Updated alert description from 'Alert description' to 'another new description'
Updated authorised by from 'A. Authorizer' to 'C Cauthorizer'
Updated active from from '${alert.activeFrom}' to '${response.activeFrom}'
Updated active to from 'null' to '${response.activeTo}'
Comment 'Another update alert' was added
""",
      )
      assertThat(actionedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
      assertThat(alertEntity.createdAt).isEqualTo(actionedAt)
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
    }
    with(alertEntity.comments().single()) {
      assertThat(comment).isEqualTo("Another update alert")
      assertThat(createdAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate updated by display name using Username header when source is NOMIS`() {
    val alert = createAlert()

    webTestClient.put()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = NOMIS, username = NOMIS_SYS_USER))
      .bodyValue(updateAlertRequest())
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByDisplayName).isEqualTo(NOMIS_SYS_USER)
    }
  }

  @Test
  fun `should publish alert updated event with ALERTS_SERVICE source`() {
    val alert = createAlert()

    webTestClient.updateAlert(alert.alertUuid, ALERTS_SERVICE, updateAlertRequest())

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()
    val updateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.alertUuid).isEqualTo(updateAlertEvent.additionalInformation.alertUuid)
    assertThat(updateAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_UPDATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          alert.prisonNumber,
          alert.alertCode.code,
          ALERTS_SERVICE,
        ),
        1,
        ALERT_UPDATED.description,
        updateAlertEvent.occurredAt,
      ),
    )
    assertThat(updateAlertEvent.occurredAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `should publish alert updated event with NOMIS source`() {
    val alert = createAlert()

    webTestClient.updateAlert(alert.alertUuid, NOMIS, updateAlertRequest())

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()
    val updateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.alertUuid).isEqualTo(updateAlertEvent.additionalInformation.alertUuid)
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
    assertThat(updateAlertEvent.occurredAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  private fun createAlertRequest(
    prisonNumber: String = PRISON_NUMBER,
    alertCode: String = ALERT_CODE_VICTIM,
  ) =
    CreateAlert(
      prisonNumber = prisonNumber,
      alertCode = alertCode,
      description = "Alert description",
      authorisedBy = "A. Authorizer",
      activeFrom = LocalDate.now().minusDays(3),
      activeTo = null,
    )

  private fun createAlert(): Alert {
    val request = createAlertRequest()
    return webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!
  }

  private fun updateAlertRequest(comment: String = "Another update alert") =
    UpdateAlert(
      description = "another new description",
      authorisedBy = "C Cauthorizer",
      activeFrom = LocalDate.now().minusMonths(2),
      activeTo = LocalDate.now().plusMonths(10),
      appendComment = comment,
    )

  private fun WebTestClient.updateAlert(
    alertUuid: UUID,
    source: Source = ALERTS_SERVICE,
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
      .returnResult().responseBody
}
