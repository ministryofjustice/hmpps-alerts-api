package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeleteAlertIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertRepository: AlertRepository

  var uuid: UUID? = null

  @BeforeEach
  fun setup() {
    uuid = UUID.randomUUID()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.delete()
      .uri("/alerts/$uuid")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.delete()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.delete()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.delete()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.delete()
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
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
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
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
      .uri("/alerts/$uuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert not found")
      assertThat(developerMessage).isEqualTo("Alert not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `alert deleted via DPS`() {
    val alert = createAlert()
    webTestClient.deleteAlert(alert.alertUuid, source = DPS)
    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity) {
      assertThat(deletedAt()).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
    with(alertEntity.auditEvents()[0]) {
      assertThat(auditEventId).isEqualTo(2)
      assertThat(action).isEqualTo(AuditEventAction.DELETED)
      assertThat(description).isEqualTo("Alert deleted")
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(DPS)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }
  }

  @Test
  fun `alert deleted via NOMIS`() {
    val alert = createAlert()
    webTestClient.deleteAlert(alert.alertUuid, source = NOMIS)
    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity) {
      assertThat(deletedAt()).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
    with(alertEntity.auditEvents()[0]) {
      assertThat(auditEventId).isEqualTo(2)
      assertThat(action).isEqualTo(AuditEventAction.DELETED)
      assertThat(description).isEqualTo("Alert deleted")
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }
  }

  @Test
  fun `should populate deleted by display name using Username header when source is NOMIS`() {
    val alert = createAlert()

    webTestClient.delete()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = NOMIS, username = NOMIS_SYS_USER))
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty

    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should populate deleted by username and display name as 'NOMIS' when source is NOMIS and no username is supplied`() {
    val alert = createAlert()

    webTestClient.delete()
      .uri("/alerts/${alert.alertUuid}")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .header(SOURCE, NOMIS.name)
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty

    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity.auditEvents()[0]) {
      assertThat(actionedBy).isEqualTo("NOMIS")
      assertThat(actionedByDisplayName).isEqualTo("Nomis")
      assertThat(source).isEqualTo(NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }
  }

  @Test
  fun `should publish alert deleted event with DPS source`() {
    val alert = createAlert()

    webTestClient.deleteAlert(alert.alertUuid, DPS)
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(deleteAlertEvent.additionalInformation.identifier())
    assertThat(deleteAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_DELETED.eventType,
        AlertAdditionalInformation(
          alert.alertUuid,
          alert.alertCode.code,
          DPS,
        ),
        1,
        ALERT_DELETED.description,
        deleteAlertEvent.occurredAt,
        "http://localhost:8080/alerts/${alert.alertUuid}",
        PersonReference.withPrisonNumber(PRISON_NUMBER),
      ),
    )
    assertThat(deleteAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertRepository.findByAlertUuidIncludingSoftDelete(
        alert.alertUuid,
      )!!.deletedAt(),
      within(1, ChronoUnit.MICROS),
    )
  }

  @Test
  fun `should publish alert deleted event with NOMIS source`() {
    val alert = createAlert()

    webTestClient.deleteAlert(alert.alertUuid, NOMIS)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 4 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(deleteAlertEvent.additionalInformation.identifier())
    assertThat(deleteAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_DELETED.eventType,
        AlertAdditionalInformation(
          alert.alertUuid,
          alert.alertCode.code,
          NOMIS,
        ),
        1,
        ALERT_DELETED.description,
        deleteAlertEvent.occurredAt,
        "http://localhost:8080/alerts/${alert.alertUuid}",
        PersonReference.withPrisonNumber(PRISON_NUMBER),
      ),
    )
    assertThat(deleteAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertRepository.findByAlertUuidIncludingSoftDelete(
        alert.alertUuid,
      )!!.deletedAt(),
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun createAlertRequest(
    alertCode: String = ALERT_CODE_VICTIM,
  ) = CreateAlert(
    alertCode = alertCode,
    description = "Alert description",
    authorisedBy = "A. Authorizer",
    activeFrom = LocalDate.now().minusDays(3),
    activeTo = null,
  )

  private fun createAlert(prisonNumber: String = PRISON_NUMBER): Alert {
    val request = createAlertRequest()
    return webTestClient.post()
      .uri("prisoners/$prisonNumber/alerts")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_WRITER), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Alert::class.java)
      .returnResult().responseBody!!
  }

  private fun WebTestClient.deleteAlert(
    alertUuid: UUID,
    source: Source = DPS,
  ) = delete()
    .uri("/alerts/$alertUuid")
    .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
    .headers(setAlertRequestContext(source = source))
    .exchange()
    .expectStatus().isNoContent
    .expectBody().isEmpty
}
