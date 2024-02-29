package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.Optional

class GetAlertTypesIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alert-types")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alert-types")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.get()
      .uri("/alert-types")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.post()
      .uri("/alert-types")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'POST' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'POST' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `get alert types excludes inactive types and codes by default`() {
    val alertTypes = webTestClient.getAlertTypes(null)
    assertThat(alertTypes.all { it.isActive }).isTrue()
    assertThat(alertTypes.flatMap { it.alertCodes }.all { it.isActive }).isTrue()
  }

  @Test
  fun `get alert types including inactive types and codes`() {
    val alertTypes = webTestClient.getAlertTypes(true)
    assertThat(alertTypes.any { !it.isActive }).isTrue()
    assertThat(alertTypes.flatMap { it.alertCodes }.any { !it.isActive }).isTrue()
  }

  private fun WebTestClient.getAlertTypes(
    includeInactive: Boolean?,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/alert-types")
          .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive))
          .build()
      }
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AlertType::class.java)
      .returnResult().responseBody!!
}
