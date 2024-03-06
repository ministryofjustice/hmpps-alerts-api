package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import com.fasterxml.jackson.module.kotlin.readValue
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
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
      assertThat(userMessage).isEqualTo("Alert not found: Could not find alert with uuid $uuid")
      assertThat(developerMessage)
        .isEqualTo("Could not find alert with uuid $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `alert deleted`() {
    val alert = createAlert()
    webTestClient.deleteAlert(alert.alertUuid)
    val alertEntity = alertRepository.findByAlertUuidIncludingSoftDelete(alert.alertUuid)!!

    with(alertEntity) {
      assertThat(deletedAt()).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
    }
    with(alertEntity.auditEvents()[0]) {
      assertThat(auditEventId).isEqualTo(2)
      assertThat(action).isEqualTo(AuditEventAction.DELETED)
      assertThat(description).isEqualTo("Alert deleted")
      assertThat(actionedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByDisplayName).isEqualTo(TEST_USER_NAME)
    }
  }

  @Test
  fun `should publish alert deleted event`() {
    val alert = createAlert()

    webTestClient.deleteAlert(alert.alertUuid)

    await untilCallTo { publishQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = objectMapper.readValue<AlertDomainEvent>(publishQueue.receiveMessageOnQueue().body())
    val deleteAlertEvent = objectMapper.readValue<AlertDomainEvent>(publishQueue.receiveMessageOnQueue().body())

    assertThat(createAlertEvent.eventType).isEqualTo(ALERT_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.alertUuid).isEqualTo(deleteAlertEvent.additionalInformation.alertUuid)
    assertThat(deleteAlertEvent).isEqualTo(
      AlertDomainEvent(
        ALERT_DELETED.eventType,
        AlertAdditionalInformation(
          "http://localhost:8080/alerts/${alert.alertUuid}",
          alert.alertUuid,
          alert.prisonNumber,
          alert.alertCode.code,
          NOMIS,
        ),
        1,
        ALERT_DELETED.description,
        deleteAlertEvent.occurredAt,
      ),
    )
    assertThat(deleteAlertEvent.occurredAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
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

  private fun WebTestClient.deleteAlert(
    alertUuid: UUID,
  ) =
    delete()
      .uri("/alerts/$alertUuid")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isNoContent
      .expectBody().isEmpty
}
