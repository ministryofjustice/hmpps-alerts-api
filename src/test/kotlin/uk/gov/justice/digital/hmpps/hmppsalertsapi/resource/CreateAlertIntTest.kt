package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.alertCodeSummary
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class CreateAlertIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.post()
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
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
      .uri("/prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).startsWith("Required request body is missing:")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = createAlertRequest()

    val response = webTestClient.createAlertResponseSpec(prisonNumber = PRISON_NUMBER_NOT_FOUND, request = request)
      .errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number not found")
      assertThat(developerMessage).isEqualTo("Prison number not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code not found`() {
    val request = createAlertRequest(alertCode = "NOT_FOUND")

    val response = webTestClient.createAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is invalid")
      assertThat(developerMessage).isEqualTo("Details => Alert code:NOT_FOUND")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - source dps - alert code is inactive`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.createAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is inactive")
      assertThat(developerMessage).isEqualTo("Alert code is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - source nomis - alert code is inactive`() {
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    webTestClient.createAlertResponseSpec(request = request, source = NOMIS)
      .expectStatus().isCreated
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().errorResponse(HttpStatus.METHOD_NOT_ALLOWED)

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
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange().errorResponse(HttpStatus.BAD_GATEWAY)

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
      .uri("prisoners/$PRISON_NUMBER_THROW_EXCEPTION/alerts")
      .bodyValue(createAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(HttpStatus.BAD_GATEWAY)

    with(response) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should populate created by using username claim`() {
    val request = createAlertRequest()
    val prisonNumber = givenPrisoner()

    val alert = webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_PRISONER_ALERTS__RW), isUserToken = false))
      .exchange().successResponse<AlertModel>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate created by using Username header`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange().successResponse<AlertModel>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should populate created by display name using Username header when source is NOMIS`() {
    val request = createAlertRequest()
    val prisonNumber = givenPrisoner()

    val alert = webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(source = NOMIS, username = NOMIS_SYS_USER))
      .exchange().successResponse<AlertModel>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val alertEntity = alertRepository.findByIdOrNull(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should populate created by username and display name as 'NOMIS' when source is NOMIS and no username is supplied`() {
    val request = createAlertRequest()
    val prisonNumber = givenPrisoner()

    val alert = webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .header(SOURCE, NOMIS.name)
      .exchange().successResponse<AlertModel>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val alertEntity = alertRepository.findByIdOrNull(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should return populated alert model`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(prisonNumber, request)

    assertThat(alert).isEqualTo(
      AlertModel(
        alert.alertUuid,
        prisonNumber,
        alertCodeSummary(),
        request.description,
        request.authorisedBy,
        request.activeFrom!!,
        request.activeTo,
        true,
        alert.createdAt,
        TEST_USER,
        TEST_USER_NAME,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        PRISON_CODE_LEEDS,
      ),
    )
    assertThat(alert.createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `201 created - source dps - allowInactiveCode=true - alert code is inactive`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    webTestClient.post().uri("/prisoners/$prisonNumber/alerts?allowInactiveCode=true")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectStatus().isCreated
  }

  @Test
  fun `should create new alert via DPS`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(prisonNumber, request)

    val alertEntity = alertRepository.findByIdOrNull(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    verifyCreatedAlert(alertEntity, alert, alertCode, prisonNumber, request, DPS)
  }

  @Test
  fun `403 forbidden - should not create new alert for restricted alert code when user does not have permission`() {
    val restrictedAlertCode = givenNewAlertCode(alertCode(code = "XXCA", restricted = true))
    val request = createAlertRequest(restrictedAlertCode.code)

    val response = webTestClient.createAlertResponseSpec(request = request).errorResponse(FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Permission denied - Permission denied for AlertCode ${restrictedAlertCode.code}")
      assertThat(developerMessage).isEqualTo("Permission denied for AlertCode ${restrictedAlertCode.code}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should create new alert for restricted alert code when user has permission`() {
    val restrictedAlertCode = givenNewAlertCode(alertCode("XXCB", restricted = true))
    givenNewAlertCodePrivilegedUser(restrictedAlertCode)
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest(restrictedAlertCode.code)

    val alert = webTestClient.createAlert(prisonNumber, request)

    val alertEntity = alertRepository.findByIdOrNull(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!
    verifyCreatedAlert(alertEntity, alert, alertCode, prisonNumber, request, DPS)
  }

  @Test
  fun `should create new alert via NOMIS`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(prisonNumber, request, NOMIS)

    val alertEntity = alertRepository.findByIdOrNull(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    verifyCreatedAlert(
      alertEntity,
      alert,
      alertCode,
      prisonNumber,
      request,
      NOMIS,
    )
  }

  @Test
  fun `should publish alert created event with DPS source`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(prisonNumber, request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        ALERT_CREATED.eventType,
        AlertAdditionalInformation(
          alert.alertUuid,
          request.alertCode,
          DPS,
        ),
        1,
        ALERT_CREATED.description,
        event.occurredAt,
        "http://localhost:8080/alerts/${alert.alertUuid}",
        PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
    assertThat(
      event.occurredAt.toLocalDateTime(),
    ).isCloseTo(alertRepository.findByIdOrNull(alert.alertUuid)!!.createdAt, within(1, ChronoUnit.MICROS))
  }

  @Test
  fun `should publish alert created event with NOMIS source`() {
    val prisonNumber = givenPrisoner()
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(prisonNumber, request, NOMIS)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        ALERT_CREATED.eventType,
        AlertAdditionalInformation(
          alert.alertUuid,
          request.alertCode,
          NOMIS,
        ),
        1,
        ALERT_CREATED.description,
        event.occurredAt,
        "http://localhost:8080/alerts/${alert.alertUuid}",
        PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
    assertThat(
      event.occurredAt.toLocalDateTime(),
    ).isCloseTo(alertRepository.findByIdOrNull(alert.alertUuid)!!.createdAt, within(1, ChronoUnit.MICROS))
  }

  @Test
  fun `409 conflict - source dps - active alert with code already exists for prison number - alert active from today with no active to date`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(alert(prisonNumber = prisonNumber, alertCode = givenAlertCode()))

    val request = createAlertRequest(alertCode = alert.alertCode.code)
    val response = webTestClient.createAlertResponseSpec(prisonNumber, request).errorResponse(CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Alert already exists")
      assertThat(developerMessage).isEqualTo("Alert already exists with identifier ${request.alertCode}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - source nomis - active alert with code already exists for prison number - alert active from today with no active to date`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(alert(prisonNumber = prisonNumber, alertCode = givenAlertCode()))

    val request = createAlertRequest(alertCode = alert.alertCode.code)
    val response = webTestClient.createAlert(prisonNumber, request, NOMIS)

    assertThat(response.alertCode.code).isEqualTo(request.alertCode)
  }

  @Test
  fun `400 bad request - active alert with code already exists for prison number - alert active from today with no active to date - alert code inactive`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(alert(prisonNumber, givenAlertCode(active = false)))

    val request = createAlertRequest(alertCode = alert.alertCode.code)
    val response = webTestClient.createAlertResponseSpec(prisonNumber, request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is inactive")
      assertThat(developerMessage).isEqualTo("Alert code is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - source dps - active alert with code already exists for prison number - alert active from tomorrow with no active to date`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(alert(prisonNumber, givenAlertCode(), activeFrom = now().plusDays(1)))

    val request = createAlertRequest(alertCode = alert.alertCode.code)

    val response = webTestClient.createAlertResponseSpec(prisonNumber, request).errorResponse(CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Alert already exists")
      assertThat(developerMessage).isEqualTo("Alert already exists with identifier ${request.alertCode}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - source nomis - active alert with code already exists for prison number - alert active from tomorrow with no active to date`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(alert(prisonNumber, givenAlertCode(), activeFrom = now().minusDays(7)))
    val request = createAlertRequest(alert.alertCode.code)

    val response = webTestClient.createAlert(prisonNumber, request, NOMIS)

    assertThat(response.alertCode.code).isEqualTo(request.alertCode)
  }

  @Test
  fun `201 created - alert with code already exists for inactive alert`() {
    val prisonNumber = givenPrisoner()
    val alert = givenAlert(
      alert(
        prisonNumber,
        givenAlertCode(),
        activeFrom = now().minusDays(7),
        activeTo = now().minusDays(2),
      ),
    )

    val request = createAlertRequest(alertCode = alert.alertCode.code)
    val response = webTestClient.createAlert(prisonNumber, request)

    assertThat(response.alertCode.code).isEqualTo(request.alertCode)
  }

  private fun createAlertRequest(alertCode: String = ALERT_CODE_VICTIM) = CreateAlert(
    alertCode = alertCode,
    description = "Alert description",
    authorisedBy = "A. Authorizer",
    activeFrom = now().minusDays(3),
    activeTo = null,
  )

  private fun WebTestClient.createAlertResponseSpec(
    prisonNumber: String = PRISON_NUMBER,
    request: CreateAlert,
    source: Source = DPS,
  ) = post()
    .uri("/prisoners/$prisonNumber/alerts")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
    .headers(setAlertRequestContext(source = source))
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)

  fun WebTestClient.createAlert(
    prisonNumber: String,
    request: CreateAlert,
    source: Source = DPS,
  ) = createAlertResponseSpec(prisonNumber, request, source).successResponse<AlertModel>(HttpStatus.CREATED)

  private fun verifyCreatedAlert(
    alertEntity: Alert,
    alert: uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert,
    alertCode: AlertCode,
    prisonNumber: String,
    request: CreateAlert,
    requestSource: Source,
  ) {
    assertThat(alertEntity).usingRecursiveComparison().ignoringFields("auditEvents", "alertCode.alertType", "version")
      .isEqualTo(
        Alert(
          id = alert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
          description = request.description,
          authorisedBy = request.authorisedBy,
          activeFrom = request.activeFrom!!,
          activeTo = request.activeTo,
          createdAt = alertEntity.createdAt,
          prisonCodeWhenCreated = PRISON_CODE_LEEDS,
        ),
      )
    with(alertEntity.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedAt).isEqualTo(alertEntity.createdAt)
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(requestSource)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }
  }
}
