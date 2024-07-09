package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alert
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeleteAlertIntTest : IntegrationTestBase() {

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
    val prisonNumber = "D1234LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

    webTestClient.deleteAlert(alert.alertUuid, source = DPS)
    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity) {
      assertThat(deletedAt()).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
    with(alertEntity.auditEvents()[0]) {
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
    val prisonNumber = "D1235LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

    webTestClient.deleteAlert(alert.alertUuid, source = NOMIS)
    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity) {
      assertThat(deletedAt()).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    }
    with(alertEntity.auditEvents()[0]) {
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
    val prisonNumber = "D1236LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

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
    val prisonNumber = "D1237LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

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
    val prisonNumber = "D1238LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

    webTestClient.deleteAlert(alert.alertUuid, DPS)
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

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
        PersonReference.withPrisonNumber(prisonNumber),
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
    val prisonNumber = "D1239LT"
    val alertCode = givenExistingAlertCode(ALERT_CODE_VICTIM)
    val alert = givenAnAlert(alert(prisonNumber, alertCode))

    webTestClient.deleteAlert(alert.alertUuid, NOMIS)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<AlertAdditionalInformation>()
    with(hmppsEventsQueue.hmppsDomainEventOnQueue()) {
      assertThat(eventType).isEqualTo(PERSON_ALERTS_CHANGED.eventType)
    }

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
        PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
    assertThat(deleteAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertRepository.findByAlertUuidIncludingSoftDelete(
        alert.alertUuid,
      )!!.deletedAt(),
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun WebTestClient.deleteAlert(alertUuid: UUID, source: Source = DPS) =
    delete()
      .uri("/alerts/$alertUuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext(source = source))
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty
}
