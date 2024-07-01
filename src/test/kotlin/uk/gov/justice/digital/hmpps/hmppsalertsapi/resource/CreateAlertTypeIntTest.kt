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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CreateAlertTypeIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertTypeRepository: AlertTypeRepository

  @BeforeEach
  fun setup() {
    val entity = alertTypeRepository.findByCode("CO")
    if (entity != null) {
      alertTypeRepository.delete(entity)
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/alert-types")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/alert-types")
      .bodyValue(createAlertTypeRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/alert-types")
      .bodyValue(createAlertTypeRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/alert-types")
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
      .uri("/alert-types")
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
      .uri("/alert-types")
      .bodyValue(createAlertTypeRequest())
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
      .uri("/alert-types")
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
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertTypesController.createAlertType(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/alert-types")
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
      .uri("/alert-types")
      .bodyValue(createAlertTypeRequest())
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
  fun `should populate created by using user_name claim`() {
    val request = createAlertTypeRequest()

    val alert = webTestClient.post()
      .uri("/alert-types")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using username claim`() {
    val request = createAlertTypeRequest()

    val alert = webTestClient.post()
      .uri("/alert-types")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = false))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should populate created by using Username header`() {
    val request = createAlertTypeRequest()

    val alert = webTestClient.post()
      .uri("/alert-types")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!

    with(alert) {
      assertThat(createdBy).isEqualTo(TEST_USER)
    }
  }

  @Test
  fun `should create new alert type`() {
    val request = createAlertTypeRequest()
    val alertType = webTestClient.createAlertType(request = request)
    assertThat(alertType).isNotNull
    assertThat(alertType.code).isEqualTo("CO")
    assertThat(alertType.description).isEqualTo("Description")
    assertThat(alertType.createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `409 conflict - active alert type exists`() {
    val request = createAlertTypeRequest()
    webTestClient.createAlertType(request)
    val response = webTestClient.createAlertTypeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Alert type exists with code '${request.code}'")
      assertThat(developerMessage).isEqualTo("Alert type exists with code '${request.code}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `validation - code too long`() {
    val request = CreateAlertTypeRequest("1234567890123", "desc")
    val response = webTestClient.createAlertTypeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage).isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertTypesController.createAlertType(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertTypeRequest' on field 'code': rejected value [1234567890123]; codes [Size.createAlertTypeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertTypeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - description too long`() {
    val request = CreateAlertTypeRequest("AB", "descdescdescdescdescdescdescdescdescdescd")
    val response = webTestClient.createAlertTypeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be between 1 & 40 characters")
      assertThat(developerMessage).isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertTypesController.createAlertType(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertTypeRequest' on field 'description': rejected value [descdescdescdescdescdescdescdescdescdescd]; codes [Size.createAlertTypeRequest.description,Size.description,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertTypeRequest.description,description]; arguments []; default message [description],40,1]; default message [Description must be between 1 & 40 characters]] ")
    }
  }

  @Test
  fun `validation - empty code`() {
    val request = CreateAlertTypeRequest("", "desc")
    val response = webTestClient.createAlertTypeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Code must be between 1 & 12 characters")
      assertThat(developerMessage).isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertTypesController.createAlertType(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertTypeRequest' on field 'code': rejected value []; codes [Size.createAlertTypeRequest.code,Size.code,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertTypeRequest.code,code]; arguments []; default message [code],12,1]; default message [Code must be between 1 & 12 characters]] ")
    }
  }

  @Test
  fun `validation - empty description`() {
    val request = CreateAlertTypeRequest("AB", "")
    val response = webTestClient.createAlertTypeResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be between 1 & 40 characters")
      assertThat(developerMessage).isEqualTo("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.AlertTypesController.createAlertType(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createAlertTypeRequest' on field 'description': rejected value []; codes [Size.createAlertTypeRequest.description,Size.description,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createAlertTypeRequest.description,description]; arguments []; default message [description],40,1]; default message [Description must be between 1 & 40 characters]] ")
    }
  }

  @Test
  fun `should publish alert type created event with DPS source`() {
    val request = createAlertTypeRequest()

    val alertType = webTestClient.createAlertType(request = request)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()

    assertThat(event).isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_CREATED.eventType,
        ReferenceDataAdditionalInformation(
          "http://localhost:8080/alert-types/${request.code}",
          request.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_CREATED.description,
        event.occurredAt,
        "http://localhost:8080/alert-types/${request.code}",
      ),
    )
    assertThat(event.occurredAt.toLocalDateTime()).isCloseTo(alertTypeRepository.findByCode(alertType.code)!!.createdAt, within(1, ChronoUnit.MICROS))
  }
  private fun createAlertTypeRequest() = CreateAlertTypeRequest("CO", "Description")

  private fun WebTestClient.createAlertTypeResponseSpec(
    request: CreateAlertTypeRequest,
  ) =
    post()
      .uri("/alert-types")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createAlertType(
    request: CreateAlertTypeRequest,
  ) =
    createAlertTypeResponseSpec(request)
      .expectStatus().isCreated
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
}
