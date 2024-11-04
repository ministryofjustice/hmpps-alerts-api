package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.alertCodeSummary
import java.util.UUID

class RetrieveAlertIntTest : IntegrationTestBase() {
  var uuid: UUID? = null

  @BeforeEach
  fun setup() {
    uuid = UUID.randomUUID()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alerts/$uuid")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `404 alert not found`() {
    val response = webTestClient.get()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().errorResponse(NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert not found")
      assertThat(developerMessage).isEqualTo("Alert not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `retrieve alert`() {
    val prisonNumber = "G1234AT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAlert(alert(prisonNumber, alertCode))

    val response = webTestClient.get()
      .uri("/alerts/${alert.id}")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .exchange().successResponse<Alert>()

    with(alert) {
      assertThat(response).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(
        Alert(
          alert.id,
          prisonNumber,
          alertCodeSummary(alertCode.alertType, alertCode),
          description,
          authorisedBy,
          activeFrom,
          activeTo,
          true,
          alert.createdAt,
          TEST_USER,
          TEST_USER_NAME,
          null,
          null,
          null,
          null,
          null,
          null,
        ),
      )
    }
  }
}
