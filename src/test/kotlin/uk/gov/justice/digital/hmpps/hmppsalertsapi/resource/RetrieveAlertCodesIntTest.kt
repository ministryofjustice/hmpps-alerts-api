package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class RetrieveAlertCodesIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alert-codes?alertTypeCode=CO")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alert-codes?alertTypeCode=CO")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.get()
      .uri("/alert-codes?alertTypeCode=CO")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/alert-codes?alertTypeCode=CO")
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
  fun `should retrieve alert codes for type`() {
    val alertCode = webTestClient.retrieveAlertCodesForType()
    assertThat(alertCode).isNotNull
    with(alertCode.find { it.code == "HID" }!!) {
      assertThat(code).isEqualTo("HID")
      assertThat(description).isEqualTo("Hidden Disability")
    }
  }

  @Test
  fun `404 no alert type found`() {
    val errorResponse = webTestClient.get()
      .uri("/alert-codes?alertTypeCode=CO")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    with(errorResponse) {
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: Alert type is inactive with code CO")
      assertThat(developerMessage).isEqualTo("Alert type is inactive with code CO")
    }
  }

  private fun WebTestClient.retrieveAlertCodesForTypeResponseSpec(code: String = "M") =
    get()
      .uri("/alert-codes?alertTypeCode=$code")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.retrieveAlertCodesForType(code: String = "M") =
    retrieveAlertCodesForTypeResponseSpec(code)
      .expectStatus().isOk
      .expectBodyList(AlertCode::class.java)
      .returnResult().responseBody!!
}
