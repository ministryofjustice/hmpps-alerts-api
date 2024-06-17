package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.MergeAlertsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERTS_MERGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason.MERGE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_ISOLATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.DEFAULT_UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.mergeAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.mergeAlerts
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

class MergeAlertsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertRepository: AlertRepository

  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/merge-alerts").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri("/merge-alerts").bodyValue(mergeAlerts()).headers(setAuthorisation()).exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post().uri("/merge-alerts").bodyValue(mergeAlerts())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER))).exchange().expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response =
      webTestClient.post().uri("/merge-alerts").headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER))).exchange()
        .expectStatus().isBadRequest.expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.mergeAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts)")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(
        mergeAlerts().copy(prisonNumberMergeFrom = ""),
        "Prison number to merge from must be supplied and be <= 10 characters",
        "prison number to merge from required",
      ),
      Arguments.of(
        mergeAlerts().copy(prisonNumberMergeFrom = 'A'.toString().repeat(11)),
        "Prison number to merge from must be supplied and be <= 10 characters",
        "prison number to merge from greater than 10",
      ),
      Arguments.of(
        mergeAlerts().copy(prisonNumberMergeTo = ""),
        "Prison number to merge to must be supplied and be <= 10 characters",
        "prison number to merge to required",
      ),
      Arguments.of(
        mergeAlerts().copy(prisonNumberMergeTo = 'A'.toString().repeat(11)),
        "Prison number to merge to must be supplied and be <= 10 characters",
        "prison number to merge to greater than 10",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(offenderBookId = 0))),
        "Offender book id must be supplied and be > 0",
        "offender book id required",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertSeq = 0))),
        "Alert sequence must be supplied and be > 0",
        "alert sequence required",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertCode = ""))),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertCode = 'a'.toString().repeat(13)))),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(description = 'a'.toString().repeat(4001)))),
        "Description must be <= 4000 characters",
        "description greater than 4000 characters",
      ),
      Arguments.of(
        mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(authorisedBy = 'a'.toString().repeat(41)))),
        "Authorised by must be <= 40 characters",
        "authorised by greater than 40 characters",
      ),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(request: MergeAlerts, expectedUserMessage: String, displayName: String) {
    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.mergeAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts): [Field error in object 'mergeAlerts' on field ")
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

    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

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

    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

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

    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

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
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.MergeAlertsController.mergeAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts) with 6 errors: [Field error in object 'mergeAlerts' on field ")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response =
      webTestClient.patch().uri("/merge-alerts").headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
        .exchange().expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED).expectBody(ErrorResponse::class.java)
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
    val response = webTestClient.post().uri("/merge-alerts")
      .bodyValue(mergeAlerts().copy(prisonNumberMergeTo = PRISON_NUMBER_THROW_EXCEPTION))
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN))).exchange().expectStatus()
      .isEqualTo(HttpStatus.BAD_GATEWAY).expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alerts to retain not found`() {
    val request = mergeAlerts(
      listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
      ),
    )
    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find alert(s) with id(s) '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'")
      assertThat(developerMessage).isEqualTo("Could not find alert(s) with id(s) '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql")
  fun `400 bad request - alerts to retain have inconsistent Prison Number`() {
    val request = mergeAlerts(
      listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("00000000-0000-0000-0000-000000000003"),
      ),
    )
    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert(s) with id(s) '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003' are not all associated with the same prison numbers")
      assertThat(developerMessage).isEqualTo("Alert(s) with id(s) '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003' are not all associated with the same prison numbers")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql(
    value = [
      "classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql",
      "classpath:test_data/additional-alerts-for-merge-test.sql",
    ],
  )
  fun `400 bad request - alerts to retain belongs to irrelevant Prison Number`() {
    val request = mergeAlerts(
      listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000003"),
        UUID.fromString("00000000-0000-0000-0000-000000000004"),
      ),
    )

    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert(s) with id(s) '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004' are not associated with either 'B2345BB' or 'A1234AA'")
      assertThat(developerMessage).isEqualTo("Alert(s) with id(s) '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004' are not associated with either 'B2345BB' or 'A1234AA'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql(
    value = [
      "classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql",
      "classpath:test_data/additional-alerts-for-merge-test.sql",
    ],
  )
  fun `400 bad request - retained alert UUIDs does not cover all alerts associated to the relevant Prison Number`() {
    val request = mergeAlerts(
      listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
      ),
    )

    val response = webTestClient.mergeAlertsResponseSpec(request = request).expectStatus().isBadRequest.expectBody(
      ErrorResponse::class.java,
    ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Retained Alert UUIDs does not cover all the alerts associated to 'B2345BB'")
      assertThat(developerMessage).isEqualTo("Retained Alert UUIDs does not cover all the alerts associated to 'B2345BB'")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest(name = "{0} allowed")
  @ValueSource(strings = [ROLE_ALERTS_ADMIN, ROLE_NOMIS_ALERTS])
  fun `201 merged - allowed role`(role: String) {
    webTestClient.mergeAlertsResponseSpec(role, mergeAlerts()).expectStatus().isCreated
  }

  @Test
  fun `returns mapping information`() {
    val offenderBookId = 54321L
    val alertSeq = 4

    val mergedAlert = webTestClient.mergeAlerts(
      request = mergeAlerts().copy(
        newAlerts = listOf(
          mergeAlert().copy(
            offenderBookId = offenderBookId,
            alertSeq = alertSeq,
          ),
        ),
      ),
    ).alertsCreated.single()

    assertThat(mergedAlert).isEqualTo(
      MergedAlert(
        offenderBookId = offenderBookId,
        alertSeq = alertSeq,
        alertUuid = mergedAlert.alertUuid,
      ),
    )
    assertThat(mergedAlert.alertUuid).isNotEqualTo(DEFAULT_UUID)
  }

  @Test
  fun `merge new alert`() {
    val request = mergeAlerts()
    val newAlert = request.newAlerts.single()

    val mergedAlert = webTestClient.mergeAlerts(request = request).alertsCreated.single()

    val alert = alertRepository.findByAlertUuid(mergedAlert.alertUuid)!!
    val alertCode = alertCodeRepository.findByCode(newAlert.alertCode)!!

    assertThat(alert).usingRecursiveAssertion().ignoringFields("auditEvents").isEqualTo(
      Alert(
        alertId = 1,
        alertUuid = alert.alertUuid,
        alertCode = alertCode,
        prisonNumber = PRISON_NUMBER,
        description = newAlert.description,
        authorisedBy = newAlert.authorisedBy,
        activeFrom = newAlert.activeFrom,
        activeTo = newAlert.activeTo,
        createdAt = alert.createdAt,
      ),
    )
    with(alert.auditEvents().single()) {
      assertThat(auditEventId).isEqualTo(1)
      assertThat(action).isEqualTo(CREATED)
      assertThat(description).isEqualTo("Alert created when merging alerts from prison number '${request.prisonNumberMergeFrom}' into prison number '${request.prisonNumberMergeTo}'")
      assertThat(actionedAt).isEqualToIgnoringNanos(alert.createdAt)
      assertThat(actionedBy).isEqualTo("SYS")
      assertThat(actionedByDisplayName).isEqualTo("Merge from ${request.prisonNumberMergeFrom}")
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  @Sql(
    value = [
      "classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql",
      "classpath:test_data/additional-alerts-for-merge-test.sql",
    ],
  )
  fun `only publish one alerts merged event when multiple alerts are created, deleted, or reassigned`() {
    val request = mergeAlerts(
      retainedAlertUuid = listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
      ),
    )

    val response = webTestClient.mergeAlerts(request = request)

    with(response) {
      assertThat(alertsDeleted).hasSize(1)
      assertThat(alertsCreated).hasSize(1)
    }

    await withPollDelay Duration.ofSeconds(2) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }

    val message = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(message.eventType).isNotEqualTo(ALERT_CREATED.eventType)
    assertThat(message.eventType).isNotEqualTo(ALERT_DELETED.eventType)

    assertThat(message).usingRecursiveComparison().ignoringFields("occurredAt", "additionalInformation.mergedAlerts.alertUuid").isEqualTo(
      AlertDomainEvent(
        eventType = ALERTS_MERGED.eventType,
        additionalInformation = MergeAlertsAdditionalInformation(
          url = "http://localhost:8080/prisoners/${request.prisonNumberMergeTo}/alerts?size=2147483647",
          prisonNumberMergeFrom = request.prisonNumberMergeFrom,
          prisonNumberMergeTo = request.prisonNumberMergeTo,
          mergedAlerts = listOf(
            MergedAlert(
              offenderBookId = 12345,
              alertSeq = 1,
              alertUuid = UUID.randomUUID(),
            ),
          ),
          source = NOMIS,
          reason = MERGE,
        ),
        version = 1,
        description = ALERTS_MERGED.description,
        occurredAt = "",
      ),
    )
  }

  @Test
  fun `merge alert with inactive alert code`() {
    val mergedAlert = webTestClient.mergeAlerts(
      request = mergeAlerts().copy(newAlerts = listOf(mergeAlert().copy(alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD))),
    ).alertsCreated.single()

    with(alertRepository.findByAlertUuid(mergedAlert.alertUuid)!!.alertCode) {
      assertThat(code).isEqualTo(ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `merge alert with active to before active from`() {
    val mergedAlert = webTestClient.mergeAlerts(
      request = mergeAlerts().copy(
        newAlerts = listOf(
          mergeAlert().copy(
            activeFrom = LocalDate.now(),
            activeTo = LocalDate.now().minusDays(1),
          ),
        ),
      ),
    ).alertsCreated.single()

    with(alertRepository.findByAlertUuid(mergedAlert.alertUuid)!!) {
      assertThat(activeTo).isBefore(activeFrom)
      assertThat(isActive()).isFalse()
    }
  }

  @Test
  fun `accepts two active alerts with the same alert code`() {
    val request = mergeAlerts().copy(
      newAlerts = listOf(
        mergeAlert().copy(
          offenderBookId = 12345,
          alertSeq = 2,
          alertCode = ALERT_CODE_ISOLATED_PRISONER,
          activeFrom = LocalDate.now().minusDays(1),
          activeTo = LocalDate.now().plusDays(1),
        ),
        mergeAlert().copy(
          offenderBookId = 54321,
          alertSeq = 4,
          alertCode = ALERT_CODE_ISOLATED_PRISONER,
          activeFrom = LocalDate.now().plusDays(1),
          activeTo = null,
        ),
      ),
    )

    val response = webTestClient.mergeAlerts(request = request)

    with(response.alertsCreated) {
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
  @Sql("classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql")
  fun `deletes merge from prisoner alerts`() {
    val prisonNumberMergeFrom = "B2345BB"
    val request = mergeAlerts().copy(prisonNumberMergeFrom = prisonNumberMergeFrom)

    val prisonNumberMergeFromAlertUuids = alertRepository.findByPrisonNumber(prisonNumberMergeFrom).map { it.alertUuid }

    val response = webTestClient.mergeAlerts(request = request)

    assertThat(response.alertsDeleted).isEqualTo(prisonNumberMergeFromAlertUuids)
    assertThat(alertRepository.findByPrisonNumber(prisonNumberMergeFrom)).isEmpty()

    await withPollDelay Duration.ofSeconds(2) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    with(hmppsEventsQueue.receiveAlertDomainEventOnQueue()) {
      assertThat(eventType).isNotEqualTo(ALERT_DELETED.eventType)
    }
  }

  @Test
  @Sql(
    value = [
      "classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql",
      "classpath:test_data/additional-alerts-for-merge-test.sql",
    ],
  )
  fun `retains alerts in prison number merge to`() {
    val request = mergeAlerts(listOf(UUID.fromString("00000000-0000-0000-0000-000000000005")))

    val retainAlertCount = alertRepository.findByPrisonNumber(request.prisonNumberMergeTo).size
    val copyAlertCount = request.newAlerts.size

    val prisonNumberMergeFromAlertUuids =
      alertRepository.findByPrisonNumber(request.prisonNumberMergeFrom).map { it.alertUuid }

    val response = webTestClient.mergeAlerts(request = request)

    assertThat(response.alertsDeleted).isEqualTo(prisonNumberMergeFromAlertUuids)
    assertThat(response.alertsCreated).hasSize(copyAlertCount)

    with(alertRepository.findByPrisonNumber(request.prisonNumberMergeTo)) {
      assertThat(this).hasSize(retainAlertCount + copyAlertCount)
      assertThat(this.map { it.alertUuid }).contains(*request.retainedAlertUuids.toTypedArray())
    }

    with(alertRepository.findByPrisonNumber(request.prisonNumberMergeFrom)) {
      assertThat(this).hasSize(0)
    }
  }

  @Test
  @Sql(
    value = [
      "classpath:test_data/existing-active-alerts-for-multiple-prison-numbers.sql",
      "classpath:test_data/additional-alerts-for-merge-test.sql",
    ],
  )
  fun `retains alerts in prison number merge from`() {
    val request = mergeAlerts(
      listOf(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
      ),
    )

    val retainAlertCount = alertRepository.findByPrisonNumber(request.prisonNumberMergeFrom).size
    val copyAlertCount = request.newAlerts.size

    val prisonNumberMergeToAlertUuids =
      alertRepository.findByPrisonNumber(request.prisonNumberMergeTo).map { it.alertUuid }

    val response = webTestClient.mergeAlerts(request = request)

    assertThat(response.alertsDeleted).isEqualTo(prisonNumberMergeToAlertUuids)
    assertThat(response.alertsCreated).hasSize(copyAlertCount)

    with(alertRepository.findByPrisonNumber(request.prisonNumberMergeTo)) {
      assertThat(this).hasSize(retainAlertCount + copyAlertCount)
      assertThat(this.map { it.alertUuid }).contains(*request.retainedAlertUuids.toTypedArray())
    }

    with(alertRepository.findByPrisonNumber(request.prisonNumberMergeFrom)) {
      assertThat(this).hasSize(0)
    }
  }

  private fun WebTestClient.mergeAlertsResponseSpec(role: String = ROLE_NOMIS_ALERTS, request: MergeAlerts) =
    post().uri("/merge-alerts").bodyValue(request).headers(setAuthorisation(roles = listOf(role))).exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.mergeAlerts(role: String = ROLE_NOMIS_ALERTS, request: MergeAlerts) =
    mergeAlertsResponseSpec(role, request).expectStatus().isCreated.expectBody(MergedAlerts::class.java)
      .returnResult().responseBody!!
}
