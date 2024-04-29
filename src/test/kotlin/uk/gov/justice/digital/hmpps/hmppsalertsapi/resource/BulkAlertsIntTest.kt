package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import com.fasterxml.jackson.module.kotlin.treeToValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.alertCodeDescriptionMap
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.bulkCreateAlertRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert as BulkAlertModel

class BulkAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Autowired
  lateinit var bulkAlertRepository: BulkAlertRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.bulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(bulkCreateAlertRequest().copy(prisonNumbers = emptyList()), "At least one prison number must be supplied", "prison numbers required"),
      Arguments.of(bulkCreateAlertRequest().copy(alertCode = ""), "Alert code must be supplied and be <= 12 characters", "alert code required"),
      Arguments.of(bulkCreateAlertRequest().copy(alertCode = 'a'.toString().repeat(13)), "Alert code must be supplied and be <= 12 characters", "alert code greater than 12 characters"),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(request: BulkCreateAlerts, expectedUserMessage: String, displayName: String) {
    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.bulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest): [Field error in object 'bulkCreateAlerts' on field ")
      assertThat(developerMessage).contains(expectedUserMessage)
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple property errors`() {
    val request = bulkCreateAlertRequest().copy(prisonNumbers = emptyList(), alertCode = "")

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        "Validation failure(s): Alert code must be supplied and be <= 12 characters\n" +
          "At least one prison number must be supplied",
      )
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.bulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest) with 2 errors:")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = bulkCreateAlertRequest().copy(alertCode = "NOT_FOUND")

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code 'NOT_FOUND' not found")
      assertThat(developerMessage).isEqualTo("Alert code 'NOT_FOUND' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code is inactive`() {
    val request = bulkCreateAlertRequest().copy(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
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
  fun `400 bad request - prisoner not found`() {
    val request = bulkCreateAlertRequest().copy(prisonNumbers = listOf(PRISON_NUMBER_NOT_FOUND))

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number(s) '$PRISON_NUMBER_NOT_FOUND' not found")
      assertThat(developerMessage).isEqualTo("Prison number(s) '$PRISON_NUMBER_NOT_FOUND' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/bulk-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
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
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest().copy(prisonNumbers = listOf(PRISON_NUMBER_THROW_EXCEPTION)))
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
  fun `creates new alert`() {
    val request = bulkCreateAlertRequest()

    val response = webTestClient.bulkCreateAlert(request)

    val createdAlert = response.alertsCreated.single()
    val alert = alertRepository.findByAlertUuid(createdAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alert).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        alertId = 1,
        alertUuid = createdAlert.alertUuid,
        alertCode = alertCode,
        prisonNumber = PRISON_NUMBER,
        description = alertCodeDescriptionMap[request.alertCode],
        authorisedBy = null,
        activeFrom = LocalDate.now(),
        activeTo = null,
        createdAt = alert.createdAt,
      ),
    )
    with(alert.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedAt).isEqualTo(alert.createdAt)
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(DPS)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }
  }

  @Test
  fun `stores and returns bulk alert with created alert`() {
    val request = bulkCreateAlertRequest()

    val response = webTestClient.bulkCreateAlert(request)

    val bulkAlert = bulkAlertRepository.findByBulkAlertUuid(response.bulkAlertUuid)!!

    assertThat(response).isEqualTo(
      BulkAlertModel(
        bulkAlertUuid = bulkAlert.bulkAlertUuid,
        request = objectMapper.treeToValue<BulkCreateAlerts>(bulkAlert.request),
        requestedAt = bulkAlert.requestedAt.withNano(0),
        requestedBy = bulkAlert.requestedBy,
        requestedByDisplayName = bulkAlert.requestedByDisplayName,
        completedAt = bulkAlert.completedAt.withNano(0),
        successful = bulkAlert.successful,
        messages = objectMapper.treeToValue<List<String>>(bulkAlert.messages),
        existingActiveAlerts = objectMapper.treeToValue<List<BulkAlertAlert>>(bulkAlert.existingActiveAlerts),
        alertsCreated = objectMapper.treeToValue<List<BulkAlertAlert>>(bulkAlert.alertsCreated),
        alertsUpdated = objectMapper.treeToValue<List<BulkAlertAlert>>(bulkAlert.alertsUpdated),
        alertsExpired = objectMapper.treeToValue<List<BulkAlertAlert>>(bulkAlert.alertsExpired),
      ),
    )
    assertThat(objectMapper.treeToValue<BulkCreateAlerts>(bulkAlert.request)).isEqualTo(request)
  }

  @Test
  fun `publishes alert created event`() {
    val request = bulkCreateAlertRequest()

    val response = webTestClient.bulkCreateAlert(request)

    val alert = alertRepository.findByAlertUuid(response.alertsCreated.single().alertUuid)!!

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        ALERT_CREATED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          alert.prisonNumber,
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
  fun `does not create new alert when existing active alert exists`() {
    val existingActiveAlert = webTestClient.createAlert(request = createAlertRequest())

    val request = bulkCreateAlertRequest()

    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.alertsCreated).isEmpty()
    with(response.existingActiveAlerts.single()) {
      assertThat(alertUuid).isEqualTo(existingActiveAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingActiveAlert.prisonNumber)
      assertThat(message).isEmpty()
    }
  }

  private fun WebTestClient.bulkCreateAlertResponseSpec(request: BulkCreateAlerts) =
    post()
      .uri("/bulk-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.bulkCreateAlert(request: BulkCreateAlerts) =
    bulkCreateAlertResponseSpec(request)
      .expectStatus().isCreated
      .expectBody(BulkAlertModel::class.java)
      .returnResult().responseBody!!

  private fun createAlertRequest() =
    CreateAlert(
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
      description = null,
      authorisedBy = null,
      activeFrom = LocalDate.now().minusDays(1),
      activeTo = null,
    )

  private fun WebTestClient.createAlert(request: CreateAlert) =
    post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
