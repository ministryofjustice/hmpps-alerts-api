package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_ADULT_AT_RISK
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_ISOLATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_POOR_COPER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.LanguageFormatUtils
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.migrateAlert
import java.time.LocalDate
import java.time.LocalDateTime

class MigratePrisonerAlertsIntTest : IntegrationTestBase() {

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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/migrate/$PRISON_NUMBER/alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
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
        migrateAlert(offenderBookId = 0),
        "Offender book id must be supplied and be > 0",
        "offender book id required",
      ),
      Arguments.of(
        migrateAlert(bookingSeq = 0),
        "Booking sequence must be supplied and be > 0",
        "booking sequence required",
      ),
      Arguments.of(
        migrateAlert(alertSeq = 0),
        "Alert sequence must be supplied and be > 0",
        "alert sequence required",
      ),
      Arguments.of(
        migrateAlert(alertCode = ""),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        migrateAlert(alertCode = 'a'.toString().repeat(13)),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
      Arguments.of(
        migrateAlert(description = 'a'.toString().repeat(4001)),
        "Description must be <= 4000 characters",
        "description greater than 4000 characters",
      ),
      Arguments.of(
        migrateAlert(authorisedBy = 'a'.toString().repeat(41)),
        "Authorised by must be <= 40 characters",
        "authorised by greater than 40 characters",
      ),
      Arguments.of(
        migrateAlert(createdBy = ""),
        "Created by must be supplied and be <= 32 characters",
        "created by required",
      ),
      Arguments.of(
        migrateAlert(createdBy = 'a'.toString().repeat(33)),
        "Created by must be supplied and be <= 32 characters",
        "created by greater than 32 characters",
      ),
      Arguments.of(
        migrateAlert(createdByDisplayName = ""),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name required",
      ),
      Arguments.of(
        migrateAlert(createdByDisplayName = 'a'.toString().repeat(256)),
        "Created by display name must be supplied and be <= 255 characters",
        "created by display name greater than 255 characters",
      ),
      Arguments.of(
        migrateAlert(lastModifiedAt = LocalDateTime.now(), lastModifiedByDisplayName = "Up Dated"),
        "Updated by is required when updated at is supplied",
        "updated by required when updated at is supplied",
      ),
      Arguments.of(
        migrateAlert(lastModifiedBy = 'a'.toString().repeat(33)),
        "Updated by must be <= 32 characters",
        "updated by greater than 32 characters",
      ),
      Arguments.of(
        migrateAlert(lastModifiedAt = LocalDateTime.now(), lastModifiedBy = "AB11DZ"),
        "Updated by display name is required when updated at is supplied",
        "updated by display name required when updated at is supplied",
      ),
      Arguments.of(
        migrateAlert(lastModifiedByDisplayName = 'a'.toString().repeat(256)),
        "Updated by display name must be <= 255 characters",
        "updated by display name greater than 255 characters",
      ),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(request: MigrateAlert, expectedUserMessage: String, displayName: String) {
    val response =
      webTestClient.migrateResponseSpec(PRISON_NUMBER, request = listOf(request)).errorResponse(BAD_REQUEST)

    with(response) {
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
      migrateAlert(alertCode = "NOT_FOUND_2"),
      migrateAlert(alertCode = "NOT_FOUND_1"),
      migrateAlert(alertCode = "NOT_FOUND_2"),
    )

    val response = webTestClient.migrateResponseSpec(PRISON_NUMBER, request = request).errorResponse(BAD_REQUEST)

    with(response) {
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
      migrateAlert(alertCode = "", createdBy = ""),
      migrateAlert(alertCode = "", authorisedBy = 'a'.toString().repeat(41)),
      migrateAlert(lastModifiedAt = LocalDateTime.now(), lastModifiedByDisplayName = "Up Dated"),
    )

    val response = webTestClient.migrateResponseSpec(PRISON_NUMBER, request = request).errorResponse(BAD_REQUEST)

    val validationMessage = """
          |Validation failures: 
          |Alert code must be supplied and be <= 12 characters
          |Authorised by must be <= 40 characters
          |Created by must be supplied and be <= 32 characters
          |Updated by is required when updated at is supplied
          |
    """.trimMargin()
    with(response) {
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
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().errorResponse(METHOD_NOT_ALLOWED)

    with(response) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'PATCH' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PATCH' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest(name = "{0} allowed")
  @ValueSource(strings = [ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI, ROLE_NOMIS_ALERTS])
  fun `201 migrated - allowed role`(role: String) {
    webTestClient.migrateResponseSpec(PRISON_NUMBER, role, emptyList()).expectStatus().isCreated
  }

  @Test
  fun `stores and returns mapping information`() {
    val offenderBookId = 54321L
    val bookingSeq = 3
    val alertSeq = 4

    val migratedAlert = webTestClient.migratePrisonerAlerts(
      "M1224SM",
      request = listOf(
        migrateAlert(
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
  }

  @Test
  fun `should migrate new alert`() {
    val prisonNumber = "M1234NA"
    val request = migrateAlert()

    val migratedAlert = webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(request.alertCode)!!

    assertThat(alert).usingRecursiveComparison()
      .ignoringFields("auditEvents", "alertCode.alertType", "comments", "createdAt")
      .isEqualTo(
        Alert(
          alertId = 1,
          alertUuid = alert.alertUuid,
          alertCode = alertCode,
          prisonNumber = prisonNumber,
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
    val prisonNumber = "M1234UA"
    val request = migrateAlert(
      lastModifiedAt = LocalDateTime.now().minusDays(1).withNano(0),
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(request)).single()

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
    val prisonNumber = "M1234IC"
    val migratedAlert =
      webTestClient.migratePrisonerAlerts(
        prisonNumber,
        request = listOf(migrateAlert(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)),
      ).single()

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!.alertCode) {
      assertThat(code).isEqualTo(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `migrate alert with active to before active from`() {
    val migratedAlert = webTestClient.migratePrisonerAlerts(
      "M1234FT",
      request = listOf(
        migrateAlert(
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
    val prisonNumber = "M1234RT"
    val createdAt = LocalDateTime.parse("2024-01-09T16:23:41.860648")
    val updatedAt = LocalDateTime.parse("2024-01-30T12:57:06.21759")
    val request = migrateAlert(
      createdAt = createdAt,
      lastModifiedAt = updatedAt,
      lastModifiedBy = "AG1221GG",
      lastModifiedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(this.createdAt).isEqualTo(createdAt)
      assertThat(lastModifiedAt).isEqualTo(updatedAt)
      assertThat(lastModifiedAuditEvent()!!.actionedAt).isEqualTo(updatedAt)
    }
  }

  @Test
  fun `should not publish alert created event`() {
    val prisonNumber = "M1234AC"
    webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(migrateAlert()))
    verify(hmppsQueueService, timeout(1000).times(0)).findByTopicId(any())
  }

  @Test
  fun `accepts two active alerts with the same alert code`() {
    val prisonNumber = "A1234DA"
    val request = listOf(
      migrateAlert(
        offenderBookId = 12345,
        bookingSeq = 1,
        alertSeq = 2,
        alertCode = ALERT_CODE_ISOLATED_PRISONER,
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = LocalDate.now().plusDays(1),
      ),
      migrateAlert(
        offenderBookId = 54321,
        bookingSeq = 3,
        alertSeq = 4,
        alertCode = ALERT_CODE_ISOLATED_PRISONER,
        activeFrom = LocalDate.now().plusDays(1),
        activeTo = null,
      ),
    )

    val response = webTestClient.migratePrisonerAlerts(prisonNumber, request = request)

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
    val prisonNumber = "M1234ID"
    val migrateExistingAlert = migrateAlert(
      offenderBookId = 12345,
      bookingSeq = 1,
      alertSeq = 2,
      alertCode = ALERT_CODE_ADULT_AT_RISK,
    )
    val migrateNewAlert = migrateAlert(
      offenderBookId = 54321,
      bookingSeq = 3,
      alertSeq = 4,
      alertCode = ALERT_CODE_POOR_COPER,
    )

    val migratedAlert =
      webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(migrateExistingAlert)).single()

    val response =
      webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(migrateExistingAlert, migrateNewAlert))

    assertThat(response).hasSize(2)
    assertThat(response.associateBy { it.alertUuid }.containsKey(migratedAlert.alertUuid)).isFalse()
  }

  @Test
  fun `deletes existing prisoner alerts`() {
    val prisonNumber = "M1234DT"
    val alert = givenAnAlert(EntityGenerator.alert(prisonNumber))
    alert.addComment("Some comment", createdBy = "ATEST", createdByDisplayName = "A Test")
    alertRepository.save(alert)

    val migratedAlert = webTestClient.migratePrisonerAlerts(prisonNumber, request = listOf(migrateAlert())).single()

    assertThat(alertRepository.findByAlertUuid(alert.alertUuid)).isNull()
    assertThat(alert.alertUuid).isNotEqualTo(migratedAlert.alertUuid)
  }

  private fun WebTestClient.migrateResponseSpec(
    prisonNumber: String,
    role: String = ROLE_NOMIS_ALERTS,
    request: Collection<MigrateAlert>,
  ) = post()
    .uri("/migrate/$prisonNumber/alerts")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(role)))
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.migratePrisonerAlerts(
    prisonNumber: String,
    role: String = ROLE_NOMIS_ALERTS,
    request: Collection<MigrateAlert>,
  ) = migrateResponseSpec(prisonNumber, role, request)
    .expectStatus().isCreated
    .expectBodyList<MigratedAlert>()
    .returnResult().responseBody!!
}
