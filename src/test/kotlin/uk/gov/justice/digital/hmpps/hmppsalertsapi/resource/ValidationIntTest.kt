package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
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
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123ASDG",
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
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "A1234AA",
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
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "A1234AA",
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
  fun `Validate prison number too long`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123ASDGA",
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
    assertThat(response.developerMessage).contains("Prison number must be <= 10 characters")
  }

  @Test
  fun `Validate description too long`() {
    var description = "a"
    for (i in 1..10) {
      description += " $description"
    }
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123AS",
          alertCode = "A",
          description = description,
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Description must be <= 1000 characters")
  }

  @Test
  fun `Validate authorised by too long`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123A",
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
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext("C. ReatedC. ReatedC. ReatedC. Rea"))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123A",
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
  fun `Update validate active to before active from`() {
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
          activeTo = LocalDate.now().minusDays(1),
          appendComment = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Active from must be before active to")
  }

  @Test
  fun `Update validate description too long`() {
    var description = "a"
    for (i in 1..10) {
      description += " $description"
    }
    val response = webTestClient.put()
      .uri("/alerts/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setAlertRequestContext())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        UpdateAlert(
          description = description,
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
    assertThat(response.developerMessage).contains("Description must be <= 1000 characters")
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
    var appendComment = "a"
    for (i in 1..10) {
      appendComment += " $appendComment"
    }
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
          appendComment = appendComment,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(response.developerMessage).contains("Append comment must be <= 1000 characters")
  }
}
