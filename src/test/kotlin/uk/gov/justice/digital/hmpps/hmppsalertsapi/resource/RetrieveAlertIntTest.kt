package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertModel

class RetrieveAlertIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

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
  fun `403 forbidden - alerts writer`() {
    webTestClient.get()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `404 alert not found`() {
    val response = webTestClient.get()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Alert not found: Could not find alert with uuid $uuid")
      assertThat(developerMessage)
        .isEqualTo("Could not find alert with uuid $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `retrieve alert`() {
    val alert = createAlert()
    val response = webTestClient.get()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .exchange()
      .expectStatus().isOk
      .expectBody(Alert::class.java)
      .returnResult().responseBody
    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(alertEntity.alertCode.code)!!

    with(response!!) {
      assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
        AlertModel(
          alertId = 1,
          alertUuid = alert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
          description = description,
          authorisedBy = authorisedBy,
          activeFrom = activeFrom,
          activeTo = activeTo,
          createdAt = alertEntity.createdAt,
        ),
      )
    }
    with(alertEntity.auditEvents()[0]) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Alert created")
      assertThat(actionedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
      assertThat(alertEntity.createdAt).isEqualTo(actionedAt)
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  private fun createAlertRequest(
    prisonNumber: String = PRISON_NUMBER,
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

  private fun createAlert(): Alert {
    val request = createAlertRequest()
    return webTestClient.post()
      .uri("/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!
  }
}
