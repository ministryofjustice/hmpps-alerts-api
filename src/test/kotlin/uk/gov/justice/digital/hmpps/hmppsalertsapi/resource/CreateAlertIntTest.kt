package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_HIDDEN_DISABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_SOCIAL_CARE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictimSummary
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class CreateAlertIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/alerts")
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
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .bodyValue(createAlertRequest())
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
    val response = webTestClient.post()
      .uri("/alerts")
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
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertsController.createAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = createAlertRequest(prisonNumber = PRISON_NUMBER_NOT_FOUND)

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number '${request.prisonNumber}' not found")
      assertThat(developerMessage).isEqualTo("Prison number '${request.prisonNumber}' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code not found`() {
    val request = createAlertRequest(alertCode = "NOT_FOUND")

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code '${request.alertCode}' not found")
      assertThat(developerMessage).isEqualTo("Alert code '${request.alertCode}' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code is inactive`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code '${request.alertCode}' is inactive")
      assertThat(developerMessage).isEqualTo("Alert code '${request.alertCode}' is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'PATCH' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PATCH' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `502 bad gateway - get user details request failed`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get user details request failed")
      assertThat(developerMessage).isEqualTo("Get user details request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `502 bad gateway - get prisoner request failed`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .bodyValue(createAlertRequest(prisonNumber = PRISON_NUMBER_THROW_EXCEPTION))
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should populate created by using user_name claim`() {
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate created by using username claim`() {
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = false))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate created by using Username header`() {
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate created by display name using Username header when source is NOMIS`() {
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = NOMIS, username = NOMIS_SYS_USER))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER)
    }
  }

  @Test
  fun `should populate created by username and display name as 'NOMIS' when source is NOMIS and no username is supplied`() {
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .header(SOURCE, NOMIS.name)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo("NOMIS")
      assertThat(createdByDisplayName).isEqualTo("NOMIS")
    }
  }

  @Test
  fun `should return populated alert model`() {
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(request = request)

    assertThat(alert).isEqualTo(
      AlertModel(
        alert.alertUuid,
        request.prisonNumber,
        alertCodeVictimSummary(),
        request.description,
        request.authorisedBy,
        request.activeFrom!!,
        request.activeTo,
        true,
        emptyList(),
        alert.createdAt,
        TEST_USER,
        TEST_USER_NAME,
        null,
        null,
        null,
      ),
    )
    assertThat(alert.createdAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `should create new alert`() {
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(request = request)

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        alertId = 1,
        alertUuid = alert.alertUuid,
        alertCode = alertCode,
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom!!,
        activeTo = request.activeTo,
        createdAt = alertEntity.createdAt,
      ),
    )
    with(alertEntity.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
      assertThat(alertEntity.createdAt).isEqualTo(actionedAt)
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should publish alert created event with DPS source`() {
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(source = DPS, request = request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        ALERT_CREATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          request.prisonNumber,
          request.alertCode,
          DPS,
        ),
        1,
        ALERT_CREATED.description,
        alert.createdAt.withNano(event.occurredAt.nano),
      ),
    )
  }

  @Test
  fun `should publish alert created event with NOMIS source`() {
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(source = NOMIS, request = request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        ALERT_CREATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          request.prisonNumber,
          request.alertCode,
          NOMIS,
        ),
        1,
        ALERT_CREATED.description,
        alert.createdAt.withNano(event.occurredAt.nano),
      ),
    )
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `409 conflict - active alert with code already exists for prison number - alert active from today with no active to date`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_HIDDEN_DISABILITY)

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(developerMessage).isEqualTo("Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `400 bad request - active alert with code already exists for prison number - alert active from today with no active to date - alert code inactive`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code '${request.alertCode}' is inactive")
      assertThat(developerMessage).isEqualTo("Alert code '${request.alertCode}' is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `409 conflict - active alert with code already exists for prison number - alert active from tomorrow with no active to date`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_SOCIAL_CARE)

    val response = webTestClient.createAlertResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(developerMessage).isEqualTo("Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `201 created - alert with code already exists for inactive alert`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_VICTIM)

    val alert = webTestClient.createAlert(request = request)

    assertThat(alert.alertCode.code).isEqualTo(request.alertCode)
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

  private fun WebTestClient.createAlertResponseSpec(
    source: Source = DPS,
    request: CreateAlert,
  ) =
    post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = source))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createAlert(
    source: Source = DPS,
    request: CreateAlert,
  ) =
    createAlertResponseSpec(source, request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
