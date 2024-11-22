package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.bulkAlertRequest

// Only failure tests in this class. Success tests are combined into BulkAlertsIntTest to validate that their results match.
class PlanBulkAlertsIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts/plan")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.planBulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest)")
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
    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

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

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failure(s): Alert code must be supplied and be <= 12 characters
          |At least one prison number must be supplied
        """.trimMargin(),
      )
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.planBulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest) with 2 errors:")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER, alertCode = "NOT_FOUND")

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

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

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

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

  private fun WebTestClient.planBulkCreateAlertResponseSpec(request: BulkCreateAlerts) =
    post()
      .uri("/bulk-alerts/plan")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
}
