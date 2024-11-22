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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.alertCodeDescriptionMap
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.bulkAlertRequest
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
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
    val response = webTestClient.bulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
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

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
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

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is invalid")
      assertThat(developerMessage).isEqualTo("Details => Alert code:NOT_FOUND")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER_NOT_FOUND)

    val response = webTestClient.bulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange()
      .errorResponse(HttpStatus.METHOD_NOT_ALLOWED)

    with(response) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange()
      .errorResponse(HttpStatus.BAD_GATEWAY)

    with(response) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange()
      .errorResponse(HttpStatus.BAD_GATEWAY)

    with(response) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `creates new active alert`() {
    val prisonNumbers = givenPrisonersExist(prisonNumber(), prisonNumber())

    val request = bulkAlertRequest(*prisonNumbers)
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    assertThat(response.alertsCreated.map { it.prisonNumber }).containsExactlyInAnyOrder(*prisonNumbers)

    response.alertsCreated.forEach { createdAlert ->
      val alert = alertRepository.findByIdOrNull(createdAlert.alertUuid)!!
      val alertCode = alertCodeRepository.findByCode(request.alertCode)!!
      assertThat(alert).usingRecursiveComparison().ignoringFields("auditEvents", "alertCode.alertType", "version")
        .isEqualTo(
          Alert(
            id = createdAlert.alertUuid,
            alertCode = alertCode,
            prisonNumber = createdAlert.prisonNumber,
            description = alertCodeDescriptionMap[request.alertCode],
            authorisedBy = null,
            activeFrom = LocalDate.now(),
            activeTo = null,
            createdAt = alert.createdAt,
            prisonCodeWhenCreated = PRISON_CODE_LEEDS,
          ),
        )
      assertThat(alert.isActive()).isTrue()
      with(alert.auditEvents().single()) {
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
  }

  @Test
  fun `creates new active alert with description`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(
      prisonNumbers = arrayOf(prisonNumber),
      alertCode = ALERT_CODE_VICTIM,
      description = "Victim alert description",
    )
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    val createdAlert = response.alertsCreated.single()
    val alert = alertRepository.findByIdOrNull(createdAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alert).usingRecursiveComparison().ignoringFields("auditEvents", "alertCode.alertType", "version")
      .isEqualTo(
        Alert(
          id = createdAlert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
          description = "Victim alert description",
          authorisedBy = null,
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdAt = alert.createdAt,
          prisonCodeWhenCreated = PRISON_CODE_LEEDS,
        ),
      )
  }

  @Test
  fun `stores and returns bulk alert with created alert`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

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
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

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
  fun `does not create new alert when existing active alert exists`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAlert(alert(prisonNumber, alertCode))

    val request = bulkAlertRequest(prisonNumber)

    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.existingActiveAlerts.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.id)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
    }

    verify(hmppsQueueService, timeout(1000).times(0)).findByTopicId(any())
  }

  @Test
  fun `clears active to date when existing active alert exists`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAlert(alert(prisonNumber, alertCode, activeTo = LocalDate.now().plusDays(1)))

    val request = bulkAlertRequest(prisonNumber)
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.alertsUpdated.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.id)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEqualTo("Updated active to from '${existingAlert.activeTo}' to 'null'")
      with(alertRepository.findByIdOrNull(alertUuid)!!) {
        assertThat(isActive()).isTrue()
        assertThat(activeTo).isNull()
      }
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.id.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `sets active from to today when existing will become active alert exists`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAlert(
      alert(
        prisonNumber,
        alertCode,
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = LocalDate.now().plusDays(2),
      ),
    )

    val request = bulkAlertRequest(prisonNumber)
    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.alertsUpdated.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.id)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEqualTo(
        "Updated active to from '${existingAlert.activeTo}' to 'null'".trimMargin(),
      )
      with(alertRepository.findByIdOrNull(alertUuid)!!) {
        assertThat(isActive()).isTrue()
        assertThat(activeFrom).isEqualTo(existingAlert.activeFrom)
        assertThat(activeTo).isNull()
      }
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()) {
      assertThat(eventType).isEqualTo(ALERT_UPDATED.eventType)
      assertThat(additionalInformation.identifier()).isEqualTo(existingAlert.id.toString())
    }
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
  }

  @Test
  fun `cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED expires existing active and will become active alerts for prison numbers not in list`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)
    val request = bulkAlertRequest(
      prisonNumber,
      cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED,
    )

    val prisonNumbersToExpire = setOf(prisonNumber(), prisonNumber())
    val toExpire = prisonNumbersToExpire.map { givenAlert(alert(it, givenExistingAlertCode(request.alertCode))) }

    val plan = webTestClient.planBulkCreateAlert(request)
    val response = webTestClient.bulkCreateAlert(request)
    plan.matchExecutionResult(response)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    with(response.alertsCreated.single()) {
      assertThat(this.prisonNumber).isEqualTo(prisonNumber)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByIdOrNull(alertUuid)!!.isActive()).isTrue()
    }
    with(response.alertsExpired) {
      assertThat(this).hasSizeGreaterThanOrEqualTo(2)
      assertThat(map { it.alertUuid }).containsAll(toExpire.map { it.id })
      assertThat(map { it.prisonNumber }).containsAll(prisonNumbersToExpire)
      onEach {
        with(alertRepository.findByIdOrNull(it.alertUuid)!!) {
          assertThat(isActive()).isFalse()
        }
      }
    }

    val messages = hmppsEventsQueue.receiveAllMessages()

    val created = messages.single { it.eventType == ALERT_CREATED.eventType }
    assertThat(created.personReference.findNomsNumber()).isEqualTo(prisonNumber)
    assertThat(created.additionalInformation["alertUuid"]).isEqualTo(response.alertsCreated.single().alertUuid.toString())

    val updated = messages.filter { it.eventType == ALERT_UPDATED.eventType }
    toExpire.forEach { alert ->
      val msg = updated.single { it.additionalInformation["alertUuid"] == alert.id.toString() }
      assertThat(msg).isNotNull
      assertThat(msg.personReference.findNomsNumber()).isEqualTo(alert.prisonNumber)
    }

    val changed = messages.filter { it.eventType == PERSON_ALERTS_CHANGED.eventType }
    assertThat(changed.map { it.personReference.findNomsNumber() }).containsAll(prisonNumbersToExpire + prisonNumber)
  }

  private fun BulkAlertPlan.matchExecutionResult(result: BulkAlertModel) {
    assertThat(result.existingActiveAlerts.map { it.prisonNumber }).containsExactlyInAnyOrder(*existingActiveAlertsPrisonNumbers.toTypedArray())
    assertThat(result.alertsCreated.map { it.prisonNumber }).containsExactlyInAnyOrder(*alertsToBeCreatedForPrisonNumbers.toTypedArray())
    assertThat(result.alertsExpired.map { it.prisonNumber }).containsExactlyInAnyOrder(*alertsToBeExpiredForPrisonNumbers.toTypedArray())
    assertThat(result.alertsUpdated.map { it.prisonNumber }).containsExactlyInAnyOrder(*alertsToBeUpdatedForPrisonNumbers.toTypedArray())
  }

  private fun WebTestClient.bulkCreateAlertResponseSpec(request: BulkCreateAlerts) =
    post()
      .uri("/bulk-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.bulkCreateAlert(request: BulkCreateAlerts) =
    bulkCreateAlertResponseSpec(request)
      .expectStatus().isCreated
      .expectBody<BulkAlertModel>()
      .returnResult().responseBody!!

  private fun WebTestClient.planBulkCreateAlertResponseSpec(request: BulkCreateAlerts) =
    post()
      .uri("/bulk-alerts/plan")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.planBulkCreateAlert(request: BulkCreateAlerts) =
    planBulkCreateAlertResponseSpec(request)
      .expectStatus().isOk
      .expectBody<BulkAlertPlan>()
      .returnResult().responseBody!!
}
