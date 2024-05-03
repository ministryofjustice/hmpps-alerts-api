package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.mergeAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.mergeAlerts
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel

class MergeAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/merge-alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/merge-alerts")
      .bodyValue(mergeAlerts())
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/merge-alerts")
      .bodyValue(mergeAlerts())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/merge-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.createAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts)")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(mergeAlerts().copy(prisonNumberMergeFrom = ""), "Prison number to merge from must be supplied and be <= 10 characters", "prison number to merge from required"),
      Arguments.of(mergeAlerts().copy(prisonNumberMergeFrom = 'A'.toString().repeat(11)), "Prison number to merge from must be supplied and be <= 10 characters", "prison number to merge from greater than 10"),
      Arguments.of(mergeAlerts().copy(prisonNumberMergeTo = ""), "Prison number to merge to must be supplied and be <= 10 characters", "prison number to merge to required"),
      Arguments.of(mergeAlerts().copy(prisonNumberMergeTo = 'A'.toString().repeat(11)), "Prison number to merge to must be supplied and be <= 10 characters", "prison number to merge to greater than 10"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(offenderBookId = 0))), "Offender book id must be supplied and be > 0", "offender book id required"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertSeq = 0))), "Alert sequence must be supplied and be > 0", "alert sequence required"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertCode = ""))), "Alert code must be supplied and be <= 12 characters", "alert code required"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertCode = 'a'.toString().repeat(13)))), "Alert code must be supplied and be <= 12 characters", "alert code greater than 12 characters"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(description = 'a'.toString().repeat(4001)))), "Description must be <= 4000 characters", "description greater than 4000 characters"),
      Arguments.of(mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(authorisedBy = 'a'.toString().repeat(41)))), "Authorised by must be <= 40 characters", "authorised by greater than 40 characters"),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(request: MergeAlerts, expectedUserMessage: String, displayName: String) {
    val response = webTestClient.mergeAlertsResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.createAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts): [Field error in object 'mergeAlerts' on field ")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = mergeAlerts().copy(
      newAlerts = listOf(
        mergeAlert(),
        mergeAlert().copy(alertCode = "NOT_FOUND_2"),
        mergeAlert().copy(alertCode = "NOT_FOUND_1"),
        mergeAlert().copy(alertCode = "NOT_FOUND_2"),
      ),
    )

    val response = webTestClient.mergeAlertsResponseSpec(request = request)
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
  fun `400 bad request - prisoner not found`() {
    val request = mergeAlerts().copy(prisonNumberMergeTo = PRISON_NUMBER_NOT_FOUND)

    val response = webTestClient.mergeAlertsResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number '${PRISON_NUMBER_NOT_FOUND}' not found")
      assertThat(developerMessage).isEqualTo("Prison number '${PRISON_NUMBER_NOT_FOUND}' not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple property errors across list of migration requests`() {
    val request = mergeAlerts().copy(
      newAlerts = listOf(
        mergeAlert(),
        mergeAlert().copy(offenderBookId = 0, alertCode = ""),
        mergeAlert().copy(alertCode = "", authorisedBy = 'a'.toString().repeat(41)),
        mergeAlert().copy(alertSeq = 0, description = 'a'.toString().repeat(4001)),
      ),
    )

    val response = webTestClient.mergeAlertsResponseSpec(request = request)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failure(s): Alert code must be supplied and be <= 12 characters
          |Alert sequence must be supplied and be > 0
          |Authorised by must be <= 40 characters
          |Description must be <= 4000 characters
          |Offender book id must be supplied and be > 0
        """.trimMargin(),
      )
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.createAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts) with 6 errors: [Field error in object 'mergeAlerts' on field ")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/merge-alerts")
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
  fun `502 bad gateway - get prisoner request failed`() {
    val response = webTestClient.post()
      .uri("/merge-alerts")
      .bodyValue(mergeAlerts().copy(prisonNumberMergeTo = PRISON_NUMBER_THROW_EXCEPTION))
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest(name = "{0} allowed")
  @ValueSource(strings = [ROLE_ALERTS_ADMIN, ROLE_NOMIS_ALERTS])
  fun `201 migrated - allowed role`(role: String) {
    webTestClient.mergeAlertsResponseSpec(role, mergeAlerts())
      .expectStatus().isCreated
  }

  /*@Test
  fun `stores and returns mapping information`() {
    val offenderBookId = 54321L
    val bookingSeq = 3
    val alertSeq = 4

    val migratedAlert = webTestClient.mergeAlerts(
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

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!.migratedAlert) {
      assertThat(this).isNotNull
      assertThat(this!!.offenderBookId).isEqualTo(offenderBookId)
      assertThat(this.bookingSeq).isEqualTo(bookingSeq)
      assertThat(this.alertSeq).isEqualTo(alertSeq)
      assertThat(this.alert.alertUuid).isEqualTo(migratedAlert.alertUuid)
      assertThat(this.migratedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))

      assertThat(this).isEqualTo(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!.migratedAlert)
    }
  }

  @Test
  fun `should migrate new alert`() {
    val request = migrateAlert()

    val migratedAlert = webTestClient.mergeAlerts(request = listOf(request)).single()

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
        migratedAt = alert.migratedAt,
      ),
    )
    with(alert.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(CREATED)
      assertThat(description).isEqualTo("Migrated alert created")
      assertThat(actionedAt).isEqualToIgnoringNanos(request.createdAt)
      assertThat(actionedBy).isEqualTo(request.createdBy)
      assertThat(actionedByDisplayName).isEqualTo(request.createdByDisplayName)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should migrate updated alert`() {
    val request = migrateAlert().copy(
      updatedAt = LocalDateTime.now().minusDays(1).withNano(0),
      updatedBy = "AG1221GG",
      updatedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.mergeAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(lastModifiedAt).isEqualTo(request.updatedAt)
      with(lastModifiedAuditEvent()!!) {
        assertThat(actionedAt).isEqualTo(request.updatedAt)
        assertThat(actionedBy).isEqualTo(request.updatedBy)
        assertThat(actionedByDisplayName).isEqualTo(request.updatedByDisplayName)
      }
    }
  }

  @Test
  fun `migrate alert with inactive alert code`() {
    val migratedAlert = webTestClient.mergeAlerts(request = listOf(migrateAlert().copy(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD))).single()

    with(alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!.alertCode) {
      assertThat(code).isEqualTo(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `migrate alert with active to before active from`() {
    val migratedAlert = webTestClient.mergeAlerts(request = listOf(migrateAlert().copy(activeFrom = LocalDate.now(), activeTo = LocalDate.now().minusDays(1)))).single()

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
      updatedAt = updatedAt,
      updatedBy = "AG1221GG",
      updatedByDisplayName = "Up Dated",
    )

    val migratedAlert = webTestClient.mergeAlerts(request = listOf(request)).single()

    val alert = alertRepository.findByAlertUuid(migratedAlert.alertUuid)!!

    with(alert) {
      assertThat(this.createdAt).isEqualTo(createdAt)
      assertThat(lastModifiedAt).isEqualTo(updatedAt)
      assertThat(lastModifiedAuditEvent()!!.actionedAt).isEqualTo(updatedAt)
    }
  }

  @Test
  fun `should not publish alert created event`() {
    webTestClient.mergeAlerts(request = listOf(migrateAlert()))
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
        activeTo = null,
      ),
      migrateAlert().copy(
        offenderBookId = 54321,
        bookingSeq = 3,
        alertSeq = 4,
        alertCode = ALERT_CODE_ISOLATED_PRISONER,
        activeFrom = LocalDate.now().minusDays(1),
        activeTo = null,
      ),
    )

    val response = webTestClient.mergeAlerts(request = request)

    with(response) {
      assertThat(this).hasSize(2)
      assertThat(this[0].alertUuid).isNotEqualTo(this[1].alertUuid)
      with(alertRepository.findByAlertUuid(this[0].alertUuid)!!) {
        assertThat(alertCode.code).isEqualTo(ALERT_CODE_ISOLATED_PRISONER)
        assertThat(isActive()).isTrue()
      }
      with(alertRepository.findByAlertUuid(this[1].alertUuid)!!) {
        assertThat(alertCode.code).isEqualTo(ALERT_CODE_ISOLATED_PRISONER)
        assertThat(isActive()).isTrue()
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

    val migratedAlert = webTestClient.mergeAlerts(request = listOf(migrateExistingAlert)).single()

    val response = webTestClient.mergeAlerts(request = listOf(migrateExistingAlert, migrateNewAlert))

    assertThat(response).hasSize(2)
    assertThat(response.associateBy { it.alertUuid }.containsKey(migratedAlert.alertUuid)).isFalse()
  }

  @Test
  fun `deletes existing prisoner alerts`() {
    var alert = webTestClient.createAlert()
    alert = webTestClient.addComment(alert.alertUuid)

    val migratedAlert = webTestClient.mergeAlerts(request = listOf(migrateAlert())).single()

    assertThat(alertRepository.findByAlertUuid(alert.alertUuid)).isNull()
    assertThat(alert.alertUuid).isNotEqualTo(migratedAlert.alertUuid)
  }*/

  private fun WebTestClient.mergeAlertsResponseSpec(role: String = ROLE_NOMIS_ALERTS, request: MergeAlerts) =
    post()
      .uri("/merge-alerts")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.mergeAlerts(role: String = ROLE_NOMIS_ALERTS, request: MergeAlerts) =
    mergeAlertsResponseSpec(role, request)
      .expectStatus().isCreated
      .expectBodyList(MigratedAlert::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.createAlert() =
    post()
      .uri("/alerts")
      .bodyValue(
        CreateAlert(
          prisonNumber = PRISON_NUMBER,
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
