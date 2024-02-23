package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import java.time.LocalDate

class ValidationIntTest : IntegrationTestBase() {

  @Test
  fun `Validate active to before active from`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123ASDG",
          alertCode = "A",
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now().minusDays(1),
          createdBy = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    assertThat(response.developerMessage).contains("Active from must be before active to")
  }

  @Test
  fun `Validate prison number too long`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123ASDGAAA",
          alertCode = "A",
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdBy = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
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
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123AS",
          alertCode = "A",
          description = description,
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdBy = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    assertThat(response.developerMessage).contains("Description must be <= 1000 characters")
  }

  @Test
  fun `Validate authorised by too long`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123A",
          alertCode = "A",
          description = "description",
          authorisedBy = "A. AuthorisedA. AuthorisedA. AuthorisedA. AuthorisedA. AuthorisedA. AuthorisedA. AuthorisedA. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdBy = null,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    assertThat(response.developerMessage).contains("Authorised by must be <= 40 characters")
  }

  @Test
  fun `Validate created by too long`() {
    val response = webTestClient.post()
      .uri("/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAlert(
          prisonNumber = "ABC123A",
          alertCode = "A",
          description = "description",
          authorisedBy = "A. Authorised",
          activeFrom = LocalDate.now(),
          activeTo = null,
          createdBy = "C. ReatedC. ReatedC. ReatedC. ReatedC. ReatedC. ReatedC. ReatedC. ReatedC. ReatedC. Reated",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    assertThat(response.developerMessage).contains("Created by must be <= 32 characters")
  }
}
