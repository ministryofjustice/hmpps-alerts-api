package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class MigratePrisonerAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .bodyValue(emptyList<MigrateAlert>())
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .bodyValue(emptyList<MigrateAlert>())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public java.util.List<uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert> uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MigratePrisonerAlertsController.createAlert(java.lang.String,java.util.List<uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert>,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code required`() {
    val request = migrateAlert().copy(alertCode = "")

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Alert code must be supplied and be <= 12 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = listOf(
      migrateAlert(),
      migrateAlert().copy(alertCode = "NOT_FOUND_2"),
      migrateAlert().copy(alertCode = "NOT_FOUND_1"),
      migrateAlert().copy(alertCode = "NOT_FOUND_2"),
    )

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code(s) 'NOT_FOUND_1', 'NOT_FOUND_2' not found")
      assertThat(developerMessage).isEqualTo("Alert code(s) 'NOT_FOUND_1', 'NOT_FOUND_2' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - description greater than 4000 characters`() {
    val request = migrateAlert().copy(description = 'a'.toString().repeat(4001))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Description must be <= 4000 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - authorised by greater than 40 characters`() {
    val request = migrateAlert().copy(authorisedBy = 'a'.toString().repeat(41))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Authorised by must be <= 40 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - active to is before active from`() {
    val request = migrateAlert().copy(activeFrom = LocalDate.now(), activeTo = LocalDate.now().minusDays(1))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Active to must be on or after active from")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - created by required`() {
    val request = migrateAlert().copy(createdBy = "")

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Created by must be supplied and be <= 32 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - created by greater than 32 characters`() {
    val request = migrateAlert().copy(createdBy = 'a'.toString().repeat(33))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Created by must be supplied and be <= 32 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - created by display name required`() {
    val request = migrateAlert().copy(createdByDisplayName = "")

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Created by display name must be supplied and be <= 255 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - created by display name greater than 255 characters`() {
    val request = migrateAlert().copy(createdByDisplayName = 'a'.toString().repeat(256))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Created by display name must be supplied and be <= 255 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - updated by required when updated at is supplied`() {
    val request = migrateAlert().copy(updatedAt = LocalDateTime.now(), updatedByDisplayName = "Up Dated")

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Updated by is required when updated at is supplied")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - updated by greater than 32 characters`() {
    val request = migrateAlert().copy(updatedBy = 'a'.toString().repeat(33))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Updated by must be <= 32 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - updated by display name required when updated at is supplied`() {
    val request = migrateAlert().copy(updatedAt = LocalDateTime.now(), updatedBy = "AB11DZ")

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Updated by display name is required when updated at is supplied")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - updated by display name greater than 255 characters`() {
    val request = migrateAlert().copy(updatedByDisplayName = 'a'.toString().repeat(256))

    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Updated by display name must be <= 255 characters")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST \"Validation failure\"")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/migrate/$PRISON_NUMBER/alerts")
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

  @ParameterizedTest
  @ValueSource(strings = [ROLE_ALERTS_ADMIN, ROLE_NOMIS_ALERTS])
  fun `201 migrated - allowed role`(role: String) {
    webTestClient.migrateResponseSpec(role, emptyList())
      .expectStatus().isCreated
  }

  /*@Test
  fun `201 created - alert code is inactive`() {
    val request = migrateAlertRequest()

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody
    assertThat(response!!.createdAt).isCloseTo(request.createdAt, within(3, ChronoUnit.SECONDS))
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
        createdAt = request.createdAt,
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
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }

    assertThat(alertEntity.migratedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
  }

  @Test
  fun `should migrate updated alert`() {
    val request = migrateAlertRequest(includeUpdate = true)

    val alert = webTestClient.migrateAlert(request = request)

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!

    with(alert) {
      assertThat(this.createdAt).isEqualTo(request.createdAt.withNano(0))
      assertThat(lastModifiedAt).isEqualTo(request.updatedAt!!.withNano(0))
    }

    with(alertEntity) {
      assertThat(this.createdAt).isEqualTo(request.createdAt)
      assertThat(lastModifiedAt).isEqualTo(request.updatedAt)
      assertThat(lastModifiedAuditEvent()!!.actionedAt).isEqualTo(request.updatedAt)
    }
  }

  @Test
  fun `migrate alert accepts and retains timestamp nanos`() {
    val createdAt = LocalDateTime.parse("2024-01-09T16:23:41.860648")
    val updatedAt = LocalDateTime.parse("2024-02-12T09:45:37.488208")
    val request = migrateAlertRequest(includeUpdate = true).copy(
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

    val alert = webTestClient.migrateAlert(request = request)

    val alertEntity = alertRepository.findByAlertUuid(alert.alertUuid)!!

    with(alert) {
      assertThat(this.createdAt).isEqualTo(createdAt.withNano(0))
      assertThat(lastModifiedAt).isEqualTo(updatedAt.withNano(0))
    }

    with(alertEntity) {
      assertThat(this.createdAt).isEqualTo(createdAt)
      assertThat(lastModifiedAt).isEqualTo(updatedAt)
      assertThat(lastModifiedAuditEvent()!!.actionedAt).isEqualTo(updatedAt)
    }
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

  @Test
  @Sql("classpath:test_data/duplicate-checking-alerts.sql")
  fun `201 created - migrate inactive alert when active alert with code already exists for prison number`() {
    val request = migrateAlertRequest(alertCode = ALERT_CODE_HIDDEN_DISABILITY, activeTo = LocalDate.now())

    val alert = webTestClient.migrateAlert(request = request)

    assertThat(alert.alertCode.code).isEqualTo(request.alertCode)
    assertThat(alert.activeTo).isEqualTo(request.activeTo)
  }*/

  private fun WebTestClient.migrateResponseSpec(role: String = ROLE_NOMIS_ALERTS, request: Collection<MigrateAlert>) =
    post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.migrateAlert(role: String = ROLE_NOMIS_ALERTS, request: Collection<MigrateAlert>) =
    migrateResponseSpec(role, request)
      .expectStatus().isCreated
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
