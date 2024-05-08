package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertTypeVulnerability
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class CreateAlertCodeIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/alert-codes")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
    val response = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(createAlertCodeRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Downstream service exception: Get user details request failed")
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
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy)
        .isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using username claim`() {
    val request = createAlertCodeRequest()

    val alert = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = false))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy)
        .isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using Username header`() {
    val request = createAlertCodeRequest()

    val alert = webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy)
        .isEqualTo(TEST_USER)
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
    val request = createAlertCodeRequest()
    webTestClient.createAlertCode(request)
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Duplicate failure: Alert code exists with code '${request.code}'")
      assertThat(developerMessage)
        .isEqualTo("Alert code exists with code '${request.code}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `validation - code too long`() {
    val request = CreateAlertCodeRequest("1234567890123", "desc", alertTypeVulnerability().code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'code': rejected value [1234567890123]; codes [Size.createAlertCodeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - parent type not found`() {
    val request = CreateAlertCodeRequest("AA", "desc", "ABCDE")
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: Alert type with code ABCDE could not be found")
      assertThat(developerMessage).isEqualTo("Alert type with code ABCDE could not be found")
    }
  }

  @Test
  fun `validation - no parent type`() {
    val request = CreateAlertCodeRequest("AA", "desc", "")
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'parent': rejected value []; codes [Size.createAlertCodeRequest.parent,Size.parent,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.parent,parent]; arguments []; default message [parent],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - parent type too long`() {
    val request = CreateAlertCodeRequest("AA", "desc", "ABCDEFGHJKLMN")
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'parent': rejected value [ABCDEFGHJKLMN]; codes [Size.createAlertCodeRequest.parent,Size.parent,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.parent,parent]; arguments []; default message [parent],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - description too long`() {
    val request =
      CreateAlertCodeRequest("AB", "descdescdescdescdescdescdescdescdescdescd", alertTypeVulnerability().code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be between 1 & 40 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'description': rejected value [descdescdescdescdescdescdescdescdescdescd]; codes [Size.createAlertCodeRequest.description,Size.description,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.description,description]; arguments []; default message [description],40,1]; default message [Description must be between 1 & 40 characters]] ")
    }
  }

  @Test
  fun `validation - empty code`() {
    val request = CreateAlertCodeRequest("", "desc", alertTypeVulnerability().code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage)
        .isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertCodesController.createAlertCode(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertCodeRequest' on field 'code': rejected value []; codes [Size.createAlertCodeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertCodeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - empty description`() {
    val request = CreateAlertCodeRequest("AB", "", alertTypeVulnerability().code)
    val response = webTestClient.createAlertCodeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
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
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_CODE_CREATED.eventType,
        ReferenceDataAdditionalInformation(
          "http://localhost:8080/alert-codes/${request.code}",
          request.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_CODE_CREATED.description,
        event.occurredAt,
      ),
    )
    assertThat(OffsetDateTime.parse(event.occurredAt).toLocalDateTime()).isCloseTo(alertCodeRepository.findByCode(alertCode.code)!!.createdAt, within(1, ChronoUnit.MICROS))
  }
  private fun createAlertCodeRequest() = CreateAlertCodeRequest("CO", "Description", alertTypeVulnerability().code)

  private fun WebTestClient.createAlertCodeResponseSpec(
    request: CreateAlertCodeRequest,
  ) =
    post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createAlertCode(
    request: CreateAlertCodeRequest,
  ) =
    createAlertCodeResponseSpec(request)
      .expectStatus().isCreated
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
}
