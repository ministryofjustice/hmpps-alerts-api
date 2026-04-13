package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
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

class ReactivateAlertTypeIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch()
      .uri("/alert-types/VI/reactivate")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.patch()
      .uri("/alert-types/VI/reactivate")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.patch()
      .uri("/alert-types/VI/reactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.patch()
      .uri("/alert-types/VI/reactivate")
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
      .uri("/alert-types/ALK/reactivate")
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
  fun `should mark alert type as active`() {
    val alertType = givenNewAlertType(alertType("ACT"))
    alertTypeRepository.findByCode(alertType.code)!!.apply {
      deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
      deactivatedBy = "MODIFIED_BY"
      alertTypeRepository.save(this)
    }

    val response = webTestClient.reactivateAlertType(alertCode = alertType.code)
    assertThat(response.isActive).isEqualTo(true)

    alertTypeRepository.findByCode(alertType.code)!!.apply {
      assertThat(deactivatedAt).isNull()
      assertThat(deactivatedBy).isNull()
    }
  }

  @Test
  fun `should publish alert types reactivated event with NOMIS source`() {
    val alertType = givenNewAlertType(alertType("REA"))
    alertTypeRepository.findByCode(alertType.code)!!.apply {
      deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
      deactivatedBy = "MODIFIED_BY"
      alertTypeRepository.save(this)
    }

    webTestClient.reactivateAlertType(alertType.code)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val reactivateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()
    assertThat(reactivateAlertEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_REACTIVATED.eventType,
        ReferenceDataAdditionalInformation(
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_REACTIVATED.description,
        reactivateAlertEvent.occurredAt,
        "http://localhost:8080/alert-types/${alertType.code}",
      ),
    )
    assertThat(reactivateAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      LocalDateTime.now(),
      within(3, ChronoUnit.SECONDS),
    )
  }

  private fun WebTestClient.reactivateAlertType(alertCode: String): AlertType = patch()
    .uri("/alert-types/$alertCode/reactivate")
    .headers(
      setAuthorisation(
        user = TEST_USER,
        roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
        isUserToken = true,
      ),
    )
    .exchange().successResponse()
}
