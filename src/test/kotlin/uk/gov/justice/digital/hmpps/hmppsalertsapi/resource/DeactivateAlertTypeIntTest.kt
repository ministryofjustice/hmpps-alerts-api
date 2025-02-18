package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeactivateAlertTypeIntTest : IntegrationTestBase() {

  var uuid: UUID? = null

  @BeforeEach
  fun setup() {
    uuid = UUID.randomUUID()
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch()
      .uri("/alert-types/VI/deactivate")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.patch()
      .uri("/alert-types/VI/deactivate")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.patch()
      .uri("/alert-types/VI/deactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.patch()
      .uri("/alert-types/VI/deactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 alert type not found`() {
    val response = webTestClient.patch()
      .uri("/alert-types/ALK/deactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert type not found")
      assertThat(developerMessage).isEqualTo("Alert type not found with identifier ALK")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should mark alert type as deactivated`() {
    val alertCode = givenNewAlertType(alertType("ABC"))
    val response = webTestClient.deleteAlertType(alertCode = alertCode.code)
    with(response) {
      assertThat(isActive).isFalse()
    }
    val entity = alertTypeRepository.findByCode(alertCode.code)
    assertThat(entity!!.deactivatedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(entity.deactivatedBy).isEqualTo(TEST_USER)
  }

  @Test
  fun `should publish alert types deactivated event with NOMIS source`() {
    val alertType = givenNewAlertType(alertType("DEF"))
    webTestClient.deleteAlertType(alertType.code)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()

    assertThat(deleteAlertEvent).isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_DEACTIVATED.eventType,
        ReferenceDataAdditionalInformation(
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_DEACTIVATED.description,
        deleteAlertEvent.occurredAt,
        "http://localhost:8080/alert-types/${alertType.code}",
      ),
    )
    assertThat(deleteAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertTypeRepository.findByCode(alertType.code)!!.deactivatedAt,
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun WebTestClient.deleteAlertType(alertCode: String): AlertType = patch()
    .uri("/alert-types/$alertCode/deactivate")
    .headers(
      setAuthorisation(
        user = TEST_USER,
        roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
        isUserToken = true,
      ),
    )
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AlertType::class.java)
    .returnResult().responseBody!!
}
