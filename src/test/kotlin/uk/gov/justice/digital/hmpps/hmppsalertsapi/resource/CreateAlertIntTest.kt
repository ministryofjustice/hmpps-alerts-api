package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictimSummary
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
      .headers(setAlertRequestContext(USER_NOT_FOUND))
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
      .headers(setAlertRequestContext(USER_THROW_EXCEPTION))
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
  fun `should return populated alert model`() {
    val request = createAlertRequest()

    val alert = webTestClient.createAlert(request)

    assertThat(alert).isEqualTo(
      AlertModel(
        alert.alertUuid,
        request.prisonNumber,
        alertCodeVictimSummary(),
        request.description,
        request.authorisedBy,
        request.activeFrom,
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

    val alert = webTestClient.createAlert(request)

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        1,
        alert.alertUuid,
        alertCode,
        request.prisonNumber,
        request.description,
        request.authorisedBy,
        request.activeFrom!!,
        request.activeTo,
      ),
    )
  }

  private fun createAlertRequest(
    prisonNumber: String = "A1234AA",
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

  private fun WebTestClient.createAlert(
    request: CreateAlert,
  ) =
    post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
