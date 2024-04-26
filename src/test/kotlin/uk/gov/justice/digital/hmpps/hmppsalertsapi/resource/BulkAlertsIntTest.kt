package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class BulkAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest())
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkCreateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.bulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  private fun bulkCreateAlertRequest() =
    BulkCreateAlerts(
      prisonNumbers = listOf(PRISON_NUMBER),
      alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
      mode = ADD_MISSING,
      cleanupMode = KEEP_ALL,
    )

  private fun WebTestClient.bulkCreateAlertResponseSpec(
    source: Source = DPS,
    request: BulkCreateAlerts,
  ) =
    post()
      .uri("/bulk-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext(source = source))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun createAlertRequest() =
    CreateAlert(
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
      description = null,
      authorisedBy = null,
      activeFrom = LocalDate.now().minusDays(1),
      activeTo = null,
    )

  private fun WebTestClient.createAlertResponseSpec(
    source: Source = DPS,
    request: CreateAlert,
  ) =
    post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = source))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createAlert(
    source: Source = DPS,
    request: CreateAlert,
  ) =
    createAlertResponseSpec(source, request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
