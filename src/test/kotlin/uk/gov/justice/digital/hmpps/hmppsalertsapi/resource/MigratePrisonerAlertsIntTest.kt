package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_ADULT_AT_RISK
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_ISOLATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_POOR_COPER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.DEFAULT_UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.LanguageFormatUtils
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
      assertThat(developerMessage).isEqualTo("Required request body is missing: public java.util.List<uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert> uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MigratePrisonerAlertsController.createAlert(java.lang.String,java.util.List<uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert>)")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(
        migrateAlert().copy(offenderBookId = 0),
        "Offender book id must be supplied and be > 0",
        "offender book id required",
      ),
      Arguments.of(
        migrateAlert().copy(bookingSeq = 0),
        "Booking sequence must be supplied and be > 0",
        "booking sequence required",
      ),
      Arguments.of(
        migrateAlert().copy(alertSeq = 0),
        "Alert sequence must be supplied and be > 0",
        "alert sequence required",
      ),
      Arguments.of(
        migrateAlert().copy(alertCode = ""),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        migrateAlert().copy(alertCode = 'a'.toString().repeat(13)),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
      Arguments.of(
        migrateAlert().copy(description = 'a'.toString().repeat(4001)),
        "Description must be <= 4000 characters",
        "description greater than 4000 characters",
      ),
      Arguments.of(
        migrateAlert().copy(authorisedBy = 'a'.toString().repeat(41)),
        "Authorised by must be <= 40 characters",
        "authorised by greater than 40 characters",
      ),
      Arguments.of(
        migrateAlert().copy(createdBy = ""),
        "Created by must be supplied and be <= 32 characters",
        "created by required",
      ),
      Arguments.of(
        migrateAlert().copy(createdBy = 'a'.toString().repeat(33)),
        "Created by must be supplied and be <= 32 characters",
        "created by greater than 32 characters",
      ),
      Arguments.of(
        migrateAlert().copy(createdByDisplayName = ""),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name required",
      ),
      Arguments.of(
        migrateAlert().copy(createdByDisplayName = 'a'.toString().repeat(256)),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name greater than 255 characters",
      ),
      Arguments.of(
        migrateAlert().copy(lastModifiedAt = LocalDateTime.now(), lastModifiedByDisplayName = "Up Dated"),
        "Updated by is required when updated at is supplied",
        "updated by required when updated at is supplied",
      ),
      Arguments.of(
        migrateAlert().copy(lastModifiedBy = 'a'.toString().repeat(33)),
        "Updated by must be <= 32 characters",
        "updated by greater than 32 characters",
      ),
      Arguments.of(
        migrateAlert().copy(lastModifiedAt = LocalDateTime.now(), lastModifiedBy = "AB11DZ"),
        "Updated by display name is required when updated at is supplied",
        "updated by display name required when updated at is supplied",
      ),
      Arguments.of(
        migrateAlert().copy(lastModifiedByDisplayName = 'a'.toString().repeat(256)),
        "Updated by display name must be <= 255 characters",
        "updated by display name greater than 255 characters",
      ),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(request: MigrateAlert, expectedUserMessage: String, displayName: String) {
    val response = webTestClient.migrateResponseSpec(request = listOf(request))
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: $expectedUserMessage")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: $expectedUserMessage")
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
  fun `400 bad request - multiple property errors across list of migration requests`() {
    val request = listOf(
      migrateAlert(),
      migrateAlert().copy(alertCode = "", createdBy = ""),
      migrateAlert().copy(alertCode = "", authorisedBy = 'a'.toString().repeat(41)),
      migrateAlert().copy(lastModifiedAt = LocalDateTime.now(), lastModifiedByDisplayName = "Up Dated"),
    )

    val response = webTestClient.migrateResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    val validationMessage = """
          |Validation failures: 
          |Alert code must be supplied and be <= 12 characters
          |Authorised by must be <= 40 characters
          |Created by must be supplied and be <= 32 characters
          |Updated by is required when updated at is supplied
          |
    """.trimMargin()
    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(validationMessage)
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST $validationMessage")
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

  @ParameterizedTest(name = "{0} allowed")
  @ValueSource(strings = [ROLE_ALERTS_ADMIN, ROLE_NOMIS_ALERTS])
  fun `201 migrated - allowed role`(role: String) {
    webTestClient.migrateResponseSpec(role, emptyList())
      .expectStatus().isCreated
  }

  @Test
  fun `stores and returns mapping information`() {
    val offenderBookId = 54321L
    val bookingSeq = 3
    val alertSeq = 4

    val migratedAlert = webTestClient.migratePrisonerAlerts(
      request = listOf(
        migrateAlert().copy(
          offenderBookId = offenderBookId,
          bookingSeq = bookingSeq,
          alertSeq = alertSeq,
        ),
      ),
    ).single()

    assertThat(migratedAlert).isEqualTo(
      MigratedAlert(
        offenderBookId = offenderBookId,
        bookingSeq = bookingSeq,
        alertSeq = alertSeq,
        alertUuid = migratedAlert.alertUuid,
      ),
    )
    assertThat(migratedAlert.alertUuid).isNotEqualTo(DEFAULT_UUID)
  }

  @Test
  fun `should migrate new alert`() {
    val request = migrateAlert()

    val migratedAlert = webTestClient.migratePrisonerAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alert).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        alertId = 1,
        alertUuid = alert.alertUuid,
        alertCode = alertCode,
        prisonNumber = PRISON_NUMBER,
        description = request.description,
        authorisedBy = request.authorisedBy,
        activeFrom = request.activeFrom,
        activeTo = request.activeTo,
        createdAt = request.createdAt,
      ),
    )
    with(alert.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(CREATED)
      assertThat(description).isEqualTo("Migrated alert created")
      assertThat(actionedAt).isEqualToIgnoringNanos(request.createdAt)
      assertThat(actionedBy).isEqualTo(request.createdBy)
      assertThat(actionedByDisplayName).isEqualTo(LanguageFormatUtils.formatDisplayName(request.createdByDisplayName))
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should migrate updated alert`() {
    val request = migrateAlert().copy(
      lastModifiedAt = LocalDateTime.now().minusDays(1).withNano(0),
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.migratePrisonerAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(lastModifiedAt).isEqualTo(request.lastModifiedAt)
      with(lastModifiedAuditEvent()!!) {
        assertThat(actionedAt).isEqualTo(request.lastModifiedAt)
        assertThat(actionedBy).isEqualTo(request.lastModifiedBy)
        assertThat(actionedByDisplayName).isEqualTo(request.lastModifiedByDisplayName)
      }
    }
  }

  @Test
  fun `migrate alert with inactive alert code`() {
    val migratedAlert =
      webTestClient.migratePrisonerAlerts(request = listOf(migrateAlert().copy(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)))
        .single()

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!.alertCode) {
      assertThat(code).isEqualTo(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `migrate alert with active to before active from`() {
    val migratedAlert = webTestClient.migratePrisonerAlerts(
      request = listOf(
        migrateAlert().copy(
          activeFrom = LocalDate.now(),
          activeTo = LocalDate.now().minusDays(1),
        ),
      ),
    ).single()

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!) {
      assertThat(activeTo).isBefore(activeFrom)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `migrate alert accepts and retains timestamp nanos`() {
    val createdAt = LocalDateTime.parse("2024-01-09T16:23:41.860648")
    val updatedAt = LocalDateTime.parse("2024-01-30T12:57:06.21759")
    val request = migrateAlert().copy(
      createdAt = createdAt,
      lastModifiedAt = updatedAt,
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.migratePrisonerAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(this.createdAt).isEqualTo(createdAt)
      assertThat(lastModifiedAt).isEqualTo(updatedAt)
      assertThat(lastModifiedAuditEvent()!!.actionedAt).isEqualTo(updatedAt)
    }
  }

  @Test
  fun `should not publish alert created event`() {
    webTestClient.migratePrisonerAlerts(request = listOf(migrateAlert()))
    Thread.sleep(1000)
    verify(hmppsQueueService, never()).findByTopicId(any())
  }

  @Test
  fun `accepts two active alerts with the same alert code`() {
    val request = listOf(
      migrateAlert().copy(
        offenderBookId = 12345,
        bookingSeq = 1,
        alertSeq = 2,
        alertCode = ALERT_CODE_ISOLATED_PRISONER,
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = LocalDate.now().plusDays(1),
      ),
      migrateAlert().copy(
        offenderBookId = 54321,
        bookingSeq = 3,
        alertSeq = 4,
        alertCode = ALERT_CODE_ISOLATED_PRISONER,
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = null,
      ),
    )

    val response = webTestClient.migratePrisonerAlerts(request = request)

    with(response) {
      assertThat(this).hasSize(2)
      assertThat(this[0].alertUuid).isNotEqualTo(this[1].alertUuid)
      with(alertRepository.findByAlertUuid(this[0].alertUuid)!!) {
        assertThat(alertCode.code).isEqualTo(ALERT_CODE_ISOLATED_PRISONER)
        assertThat(isActive()).isTrue()
      }
      with(alertRepository.findByAlertUuid(this[1].alertUuid)!!) {
        assertThat(alertCode.code).isEqualTo(ALERT_CODE_ISOLATED_PRISONER)
      }
    }
  }

  @Test
  fun `migration is idempotent`() {
    val migrateExistingAlert = migrateAlert().copy(
      offenderBookId = 12345,
      bookingSeq = 1,
      alertSeq = 2,
      alertCode = ALERT_CODE_ADULT_AT_RISK,
    )
    val migrateNewAlert = migrateAlert().copy(
      offenderBookId = 54321,
      bookingSeq = 3,
      alertSeq = 4,
      alertCode = ALERT_CODE_POOR_COPER,
    )

    val migratedAlert = webTestClient.migratePrisonerAlerts(request = listOf(migrateExistingAlert)).single()

    val response = webTestClient.migratePrisonerAlerts(request = listOf(migrateExistingAlert, migrateNewAlert))

    assertThat(response).hasSize(2)
    assertThat(response.associateBy { it.alertUuid }.containsKey(migratedAlert.alertUuid)).isFalse()
  }

  @Test
  fun `deletes existing prisoner alerts`() {
    var alert = webTestClient.createAlert()
    alert = webTestClient.addComment(alert.alertUuid)

    val migratedAlert = webTestClient.migratePrisonerAlerts(request = listOf(migrateAlert())).single()

    assertThat(alertRepository.findByAlertUuid(alert.alertUuid)).isNull()
    assertThat(alert.alertUuid).isNotEqualTo(migratedAlert.alertUuid)
  }

  private fun WebTestClient.migrateResponseSpec(role: String = ROLE_NOMIS_ALERTS, request: Collection<MigrateAlert>) =
    post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.migratePrisonerAlerts(role: String = ROLE_NOMIS_ALERTS, request: Collection<MigrateAlert>) =
    migrateResponseSpec(role, request)
      .expectStatus().isCreated
      .expectBodyList(MigratedAlert::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.createAlert(prisonNumber: String = PRISON_NUMBER) =
    post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(
        CreateAlert(
          alertCode = ALERT_CODE_VICTIM,
          description = "Alert description",
          authorisedBy = "A. Authorizer",
          activeFrom = LocalDate.now().minusDays(3),
          activeTo = null,
        ),
      )
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.addComment(alertUuid: UUID) =
    put()
      .uri("/alerts/$alertUuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlert(appendComment = "Additional comment"))
      .exchange()
      .expectBody(AlertModel::class.java)
      .returnResult().responseBody!!
}
