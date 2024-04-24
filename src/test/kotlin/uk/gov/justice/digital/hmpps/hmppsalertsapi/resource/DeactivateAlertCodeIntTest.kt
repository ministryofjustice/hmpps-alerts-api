package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_VULNERABILITY
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeactivateAlertCodeIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  var uuid: UUID? = null

  @BeforeEach
  fun setup() {
    uuid = UUID.randomUUID()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.delete()
      .uri("/alert-codes/VI")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.delete()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.delete()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.delete()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
    val response = webTestClient.delete()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
  fun `404 alert not found`() {
    val response = webTestClient.delete()
      .uri("/alert-codes/ALK")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert with code ALK could not be found")
      assertThat(developerMessage)
        .isEqualTo("Alert with code ALK could not be found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should mark alert as deactivated`() {
    val alertCode = createAlertCode()
    val response = webTestClient.deleteAlertCode(alertCode = alertCode.code)
    assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT)
    val entity = alertCodeRepository.findByCode(alertCode.code)
    assertThat(entity!!.deactivatedAt).isCloseTo(LocalDateTime.now(), Assertions.within(3, ChronoUnit.SECONDS))
    assertThat(entity.deactivatedBy).isEqualTo(TEST_USER)
  }

  private fun createAlertCodeRequest(
    alertType: String = ALERT_TYPE_CODE_VULNERABILITY,
    alertCode: String = ALERT_CODE_VICTIM,
  ) =
    CreateAlertCodeRequest(alertCode, "description", alertType)

  private fun createAlertCode(): AlertCode {
    val request = createAlertCodeRequest(alertCode = "ABC")
    return webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
  }

  private fun WebTestClient.deleteAlertCode(
    alertCode: String,
  ) =
    delete()
      .uri("/alert-codes/$alertCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty
}
