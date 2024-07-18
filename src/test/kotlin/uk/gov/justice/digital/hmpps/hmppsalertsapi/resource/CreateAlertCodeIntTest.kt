package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_GATEWAY
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AT_VULNERABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CreateAlertCodeIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setup() {
    val entity = alertCodeRepository.findByCode("CO")
    if (entity != null) {
      alertCodeRepository.delete(entity)
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/alert-codes")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(createAlertCodeRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(createAlertCodeRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange().errorResponse()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(developerMessage)
        .isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.post()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().errorResponse()

    with(response) {
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
    val response = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(createAlertCodeRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .exchange().errorResponse()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage)
        .isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage)
        .isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().errorResponse(METHOD_NOT_ALLOWED)

    with(response) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Method not allowed failure: Request method 'PATCH' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PATCH' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `502 bad gateway - get user details request failed`() {
    val response = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(createAlertCodeRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange().errorResponse(BAD_GATEWAY)

    with(response) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get user details request failed")
      assertThat(developerMessage).isEqualTo("Get user details request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should populate created by using user_name claim`() {
    val request = createAlertCodeRequest()

    val alert = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .exchange()
      .successResponse<AlertCode>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using username claim`() {
    val request = createAlertCodeRequest()

    val alert = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = false,
        ),
      )
      .exchange()
      .successResponse<AlertCode>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using Username header`() {
    val request = createAlertCodeRequest()

    val alert = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange()
      .successResponse<AlertCode>(HttpStatus.CREATED)

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should create new alert code`() {
    val request = createAlertCodeRequest()
    val alertCode = webTestClient.createAlertCode(request = request)
    assertThat(alertCode).isNotNull
    assertThat(alertCode.code).isEqualTo("CO")
    assertThat(alertCode.description).isEqualTo("Description")
    assertThat(alertCode.createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `409 conflict - alert code exists`() {
    val alertCode = givenNewAlertCode(alertCode("EXIST"))
    val request = createAlertCodeRequest(alertCode.code)

    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse(CONFLICT)
    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Alert code already exists")
      assertThat(developerMessage).isEqualTo("Alert code already exists with identifier ${request.code}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `validation - code too long`() {
    val request = CreateAlertCodeRequest("1234567890123", "desc", AT_VULNERABILITY.code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'code': rejected value [1234567890123]; codes [Size.createAlertCodeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - parent type not found`() {
    val request = CreateAlertCodeRequest("AA", "desc", "ABCDE")
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse(NOT_FOUND)
    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: Alert type not found")
      assertThat(developerMessage).isEqualTo("Alert type not found with identifier ABCDE")
    }
  }

  @Test
  fun `validation - no parent type`() {
    val request = CreateAlertCodeRequest("AA", "desc", "")
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'parent': rejected value []; codes [Size.createAlertCodeRequest.parent,Size.parent,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.parent,parent]; arguments []; default message [parent],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - parent type too long`() {
    val request = CreateAlertCodeRequest("AA", "desc", "ABCDEFGHJKLMN")
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'parent': rejected value [ABCDEFGHJKLMN]; codes [Size.createAlertCodeRequest.parent,Size.parent,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.parent,parent]; arguments []; default message [parent],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - description too long`() {
    val request =
      CreateAlertCodeRequest("AB", "descdescdescdescdescdescdescdescdescdescd", AT_VULNERABILITY.code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be between 1 & 40 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'description': rejected value [descdescdescdescdescdescdescdescdescdescd]; codes [Size.createAlertCodeRequest.description,Size.description,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.description,description]; arguments []; default message [description],40,1]; default message [Description must be between 1 & 40 characters]] ")
    }
  }

  @Test
  fun `validation - empty code`() {
    val request = CreateAlertCodeRequest("", "desc", AT_VULNERABILITY.code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'code': rejected value []; codes [Size.createAlertCodeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - empty description`() {
    val request = CreateAlertCodeRequest("AB", "", AT_VULNERABILITY.code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request).errorResponse()
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be between 1 & 40 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'description': rejected value []; codes [Size.createAlertCodeRequest.description,Size.description,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.description,description]; arguments []; default message [description],40,1]; default message [Description must be between 1 & 40 characters]] ")
    }
  }

  @Test
  fun `should publish alert code created event with DPS source`() {
    val request = createAlertCodeRequest()

    val alertCode = webTestClient.createAlertCode(request = request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_CODE_CREATED.eventType,
        ReferenceDataAdditionalInformation(
          request.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_CODE_CREATED.description,
        event.occurredAt,
        "http://localhost:8080/alert-codes/${request.code}",
      ),
    )
    assertThat(event.occurredAt.toLocalDateTime()).isCloseTo(
      alertCodeRepository.findByCode(alertCode.code)!!.createdAt,
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun createAlertCodeRequest(code: String = "CO", description: String = "Description") =
    CreateAlertCodeRequest(code, description, AT_VULNERABILITY.code)

  private fun WebTestClient.createAlertCodeResponseSpec(request: CreateAlertCodeRequest) =
    post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
        ),
      )
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createAlertCode(request: CreateAlertCodeRequest) =
    createAlertCodeResponseSpec(request)
      .expectStatus().isCreated
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
}
