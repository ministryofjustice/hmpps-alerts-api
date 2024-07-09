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
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.alertCodeDescriptionMap
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.EXPIRE_AND_REPLACE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.bulkAlertRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert as BulkAlertModel

class BulkAlertsIntTest : IntegrationTestBase() {

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
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
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
      Arguments.of(
        bulkAlertRequest(),
        "At least one prison number must be supplied",
        "prison numbers required",
      ),
      Arguments.of(
        bulkAlertRequest(PRISON_NUMBER, alertCode = ""),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        bulkAlertRequest(PRISON_NUMBER, alertCode = 'a'.toString().repeat(13)),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(
    request: BulkCreateAlerts,
    expectedUserMessage: String,
    displayName: String,
  ) {
    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).contains(expectedUserMessage)
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple property errors`() {
    val request = bulkAlertRequest(alertCode = "")

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failure(s): Alert code must be supplied and be <= 12 characters
          |At least one prison number must be supplied
        """.trimMargin(),
      )
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.bulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest) with 2 errors:")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER, alertCode = "NOT_FOUND")

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is invalid")
      assertThat(developerMessage).isEqualTo("Details => Alert code:NOT_FOUND")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code is inactive`() {
    val request = bulkAlertRequest(PRISON_NUMBER, alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is inactive")
      assertThat(developerMessage).isEqualTo("Alert code is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER_NOT_FOUND)

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number(s) not found")
      assertThat(developerMessage).isEqualTo("Prison number(s) not found")
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
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
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
      .bodyValue(bulkAlertRequest(PRISON_NUMBER_THROW_EXCEPTION))
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
  fun `creates new active alert`() {
    val prisonNumber = "B1234LK"
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val response = webTestClient.bulkCreateAlert(request)

    val createdAlert = response.alertsCreated.single()
    val alert = alertRepository.findByAlertUuid(createdAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alert).usingRecursiveComparison().ignoringFields("auditEvents", "alertCode.alertType", "comments")
      .isEqualTo(
        Alert(
          alertId = 1,
          alertUuid = createdAlert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
          description = alertCodeDescriptionMap[request.alertCode],
          authorisedBy = null,
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdAt = alert.createdAt,
        ),
      )
    assertThat(alert.isActive()).isTrue()
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
    val prisonNumber = "B1235LK"
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
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
    val prisonNumber = "B1236LK"
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val response = webTestClient.bulkCreateAlert(request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(response.alertsCreated.single().alertUuid.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `mode = ADD_MISSING does not create new alert when existing active alert exists`() {
    val prisonNumber = "B1237LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAnAlert(alert(prisonNumber, alertCode))

    val request = bulkAlertRequest(prisonNumber, mode = ADD_MISSING)

    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.existingActiveAlerts.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
    }

    verify(hmppsQueueService, timeout(1000).times(0)).findByTopicId(any())
  }

  @Test
  fun `mode = ADD_MISSING clears active to date when existing active alert exists`() {
    val prisonNumber = "B1238LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAnAlert(alert(prisonNumber, alertCode, activeTo = LocalDate.now().plusDays(1)))

    val request = bulkAlertRequest(prisonNumber, mode = ADD_MISSING)
    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.alertsUpdated.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEqualTo("Updated active to from '${existingAlert.activeTo}' to 'null'")
      with(alertRepository.findByAlertUuid(alertUuid)!!) {
        assertThat(isActive()).isTrue()
        assertThat(activeTo).isNull()
      }
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.alertUuid.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `mode = ADD_MISSING sets active from to today when existing will become active alert exists`() {
    val prisonNumber = "B1239LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAnAlert(
      alert(
        prisonNumber,
        alertCode,
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = LocalDate.now().plusDays(2),
      ),
    )

    val request = bulkAlertRequest(prisonNumber, mode = ADD_MISSING)
    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.alertsUpdated.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEqualTo(
        "Updated active to from '${existingAlert.activeTo}' to 'null'".trimMargin(),
      )
      with(alertRepository.findByAlertUuid(alertUuid)!!) {
        assertThat(isActive()).isTrue()
        assertThat(activeFrom).isEqualTo(existingAlert.activeFrom)
        assertThat(activeTo).isNull()
      }
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.alertUuid.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `mode = EXPIRE_AND_REPLACE expires existing active alert and creates new active alert to replace it`() {
    val prisonNumber = "B1240LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAnAlert(alert(prisonNumber, alertCode))

    val request = bulkAlertRequest(prisonNumber, mode = EXPIRE_AND_REPLACE)

    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    with(response.alertsCreated.single()) {
      assertThat(alertUuid).isNotEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByAlertUuid(alertUuid)!!.isActive()).isTrue()
    }
    with(response.alertsExpired.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByAlertUuid(alertUuid)!!.isActive()).isFalse()
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.alertUuid.toString())
    }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(response.alertsCreated.single().alertUuid.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `mode = EXPIRE_AND_REPLACE expires existing will become active alert and creates new active alert to replace it`() {
    val prisonNumber = "B1241LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAnAlert(alert(prisonNumber, alertCode, activeFrom = LocalDate.now().plusDays(1)))

    val request = bulkAlertRequest(prisonNumber, mode = EXPIRE_AND_REPLACE)
    val response = webTestClient.bulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    with(response.alertsCreated.single()) {
      assertThat(alertUuid).isNotEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByAlertUuid(alertUuid)!!.isActive()).isTrue()
    }
    with(response.alertsExpired.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.alertUuid)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
      with(alertRepository.findByAlertUuid(alertUuid)!!) {
        assertThat(isActive()).isFalse()
        assertThat(activeTo).isEqualTo(LocalDate.now())
      }
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.alertUuid.toString())
    }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_CREATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(response.alertsCreated.single().alertUuid.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  @Sql("classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql")
  fun `cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED expires existing active and will become active alerts for prison numbers not in list`() {
    val request = bulkAlertRequest(
      PRISON_NUMBER,
      mode = ADD_MISSING,
      cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED,
    )

    val response = webTestClient.bulkCreateAlert(request)

    val alertsForFirstPrisonerNotInList = alertRepository.findByPrisonNumber("B2345BB")
    val alertsForSecondPrisonerNotInList = alertRepository.findByPrisonNumber("C3456CC")
    val alertsExpiredForPrisonersNotInList =
      alertsForFirstPrisonerNotInList.union(alertsForSecondPrisonerNotInList).filterNot { it.isActive() }

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    with(response.alertsCreated.single()) {
      assertThat(prisonNumber).isEqualTo(PRISON_NUMBER)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByAlertUuid(alertUuid)!!.isActive()).isTrue()
    }
    with(response.alertsExpired) {
      assertThat(this).hasSize(2)
      assertThat(map { it.alertUuid }).containsAll(alertsExpiredForPrisonersNotInList.map { it.alertUuid })
      assertThat(map { it.prisonNumber }).contains("B2345BB", "C3456CC")
      onEach {
        with(alertRepository.findByAlertUuid(it.alertUuid)!!) {
          assertThat(isActive()).isFalse()
        }
      }
    }

    assertThat(alertsForFirstPrisonerNotInList.single { it.isActive() }.alertCode.code).isEqualTo("ADSC")
    assertThat(alertsForSecondPrisonerNotInList.filter { it.isActive() }).isEmpty()

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 6 }
    val messages = listOf(
      hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>(),
      hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>(),
      hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>(),
    )

    with(messages.filter { it.eventType == ALERT_UPDATED.eventType }) {
      assertThat(this.map { it.additionalInformation.identifier() }).containsExactlyInAnyOrder(
        alertsExpiredForPrisonersNotInList[0].alertUuid.toString(),
        alertsExpiredForPrisonersNotInList[1].alertUuid.toString(),
      )
    }

    with(messages.filter { it.eventType == ALERT_CREATED.eventType }) {
      assertThat(this.map { it.additionalInformation.identifier() }).containsExactlyInAnyOrder(
        response.alertsCreated.single().alertUuid.toString(),
      )
    }
    repeat(3) {
      with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
        assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
      }
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
}
