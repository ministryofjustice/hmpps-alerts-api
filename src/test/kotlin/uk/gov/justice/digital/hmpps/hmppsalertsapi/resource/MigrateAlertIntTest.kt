package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_HIDDEN_DISABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_SOCIAL_CARE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.alertCodeVictimSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlertRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class MigrateAlertIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/migrate/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/migrate/alerts")
      .bodyValue(migrateAlertRequest())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/migrate/alerts")
      .bodyValue(migrateAlertRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/migrate/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MigrateAlertsController.createAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code not found`() {
    val request = migrateAlertRequest(alertCode = "NOT_FOUND")

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code '${request.alertCode}' not found")
      assertThat(developerMessage).isEqualTo("Alert code '${request.alertCode}' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - alert code is inactive`() {
    val request = migrateAlertRequest()

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody
    assertThat(response!!.createdAt).isCloseTo(request.createdAt, within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/migrate/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'PATCH' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PATCH' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should return populated alert model`() {
    val request = migrateAlertRequest()

    val alert = webTestClient.migrateAlert(request = request)
    val expectedCreatedTime = LocalDateTime.of(request.createdAt.year, request.createdAt.month, request.createdAt.dayOfMonth, request.createdAt.hour, request.createdAt.minute, request.createdAt.second)
    assertThat(alert).isEqualTo(
      AlertModel(
        alert.alertUuid,
        request.prisonNumber,
        alertCodeVictimSummary(),
        request.description,
        request.authorisedBy,
        request.activeFrom,
        request.activeTo,
        true,
        emptyList(),
        expectedCreatedTime,
        request.createdBy,
        request.createdByDisplayName,
        null,
        null,
        null,
      ),
    )
  }

  @Test
  fun `should migrate new alert`() {
    val request = migrateAlertRequest()

    val alert = webTestClient.migrateAlert(request = request)

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alertEntity).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        alertId = 1,
        alertUuid = alert.alertUuid,
        alertCode = alertCode,
        prisonNumber = request.prisonNumber,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        createdAt = alert.createdAt,
        migratedAt = alertEntity.migratedAt,
      ),
    )
    with(alertEntity.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Migrated alert created")
      assertThat(actionedAt).isEqualToIgnoringNanos(request.createdAt)
      assertThat(actionedBy).isEqualTo("AG111QD")
      assertThat(actionedByDisplayName).isEqualTo("A Creator")
    }

    assertThat(alertEntity.migratedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `should not publish alert created event`() {
    val request = migrateAlertRequest()

    webTestClient.migrateAlert(request = request)
    Thread.sleep(1000)
    verify(hmppsQueueService, never()).findByTopicId(any())
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `409 conflict - active alert with code already exists for prison number - alert active from today with no active to date`() {
    val request = migrateAlertRequest(alertCode = ALERT_CODE_HIDDEN_DISABILITY)

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(developerMessage).isEqualTo("Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `409 bad request - active alert with code already exists for prison number - alert active from today with no active to date - alert code inactive`() {
    val request = migrateAlertRequest(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Active alert with code 'URS' already exists for prison number 'A1234AA'")
      assertThat(developerMessage).isEqualTo("Active alert with code 'URS' already exists for prison number 'A1234AA'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `409 conflict - active alert with code already exists for prison number - alert active from tomorrow with no active to date`() {
    val request = migrateAlertRequest(alertCode = ALERT_CODE_SOCIAL_CARE)

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Duplicate failure: Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(developerMessage).isEqualTo("Active alert with code '${request.alertCode}' already exists for prison number '${request.prisonNumber}'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `201 created - alert with code already exists for inactive alert`() {
    val request = migrateAlertRequest()

    val alert = webTestClient.migrateAlert(request = request)

    assertThat(alert.alertCode.code).isEqualTo(request.alertCode)
  }

  private fun WebTestClient.migrateResponseSpec(request: MigrateAlertRequest) =
    post()
      .uri("/migrate/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.migrateAlert(request: MigrateAlertRequest) =
    migrateResponseSpec(request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
