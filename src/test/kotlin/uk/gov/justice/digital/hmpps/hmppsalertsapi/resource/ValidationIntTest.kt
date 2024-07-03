package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

class ValidationIntTest : IntegrationTestBase() {

  @Test
  fun `Validate active to before active from`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = "A",
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now().minusDays(1),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Active from must be before active to")
  }

  @Test
  fun `Validate active from is equal to active to should pass on creation`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = ALERT_CODE_VICTIM,
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now(),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody(Alert::class.java)
      .returnResult().responseBody
    with(response!!) {
      assertThat(activeFrom).isEqualTo(activeTo)
    }
  }

  @Test
  fun `Validate active from is equal to active to should pass on update`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = ALERT_CODE_VICTIM,
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now(),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!

    val updateResponse = webTestClient.put()
      .uri("/alerts/${response.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        UpdateAlert(
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now(),
          appendComment = null,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody
    with(updateResponse!!) {
      assertThat(activeFrom).isEqualTo(activeTo)
    }
  }

  @Test
  fun `Validate description too long`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = "A",
          description = "a".repeat(4001),
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Description must be <= 4000 characters")
  }

  @Test
  fun `Validate authorised by too long`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = "A",
          description = "description",
          authorisedBy = "A. AuthorisedA. AuthorisedA. AuthorisedA.",
          activeFrom = LocalDate.now(),
          activeTo = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Authorised by must be <= 40 characters")
  }

  @Test
  fun `Validate created by too long`() {
    val response = webTestClient.post()
      .uri("prisoners/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext(username = "C. ReatedC. ReatedC. ReatedC. Rea"))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          alertCode = "A",
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Created by must be <= 32 characters")
  }

  @Test
  fun `Update validate description too long`() {
    val response = webTestClient.put()
      .uri("/alerts/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        UpdateAlert(
          description = "a".repeat(4001),
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          appendComment = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Description must be <= 4000 characters")
  }

  @Test
  fun `Update validate authorised by too long`() {
    val response = webTestClient.put()
      .uri("/alerts/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        UpdateAlert(
          description = "description",
          authorisedBy = "A. AuthorisedA. AuthorisedA. AuthorisedA.",
          activeFrom = LocalDate.now(),
          activeTo = null,
          appendComment = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Authorised by must be <= 40 characters")
  }

  @Test
  fun `Update validate append comment too long`() {
    val response = webTestClient.put()
      .uri("/alerts/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        UpdateAlert(
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          appendComment = "a".repeat(1001),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Append comment must be <= 1000 characters")
  }
}
