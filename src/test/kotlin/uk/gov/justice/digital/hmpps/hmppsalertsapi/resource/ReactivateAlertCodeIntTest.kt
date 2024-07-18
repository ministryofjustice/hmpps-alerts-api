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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_VULNERABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReactivateAlertCodeIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
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
    val response = webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
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
      .uri("/alert-codes/ALK/reactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert code not found")
      assertThat(developerMessage).isEqualTo("Alert code not found with identifier ALK")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should mark alert type as active`() {
    val alertType = givenExistingAlertType(ALERT_TYPE_CODE_VULNERABILITY)
    val alertCode = givenNewAlertCode(alertCode("ACT", type = alertType))
    alertCodeRepository.findByCode(alertCode.code)!!.apply {
      deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
      deactivatedBy = "MODIFIED_BY"
      alertCodeRepository.save(this)
    }

    val response = webTestClient.reactivateAlertCode(alertCode = alertCode.code)
    assertThat(response.isActive).isEqualTo(true)

    alertCodeRepository.findByCode(alertCode.code)!!.apply {
      assertThat(deactivatedAt).isNull()
      assertThat(deactivatedBy).isNull()
    }
  }

  @Test
  fun `should publish alert types reactivated event with NOMIS source`() {
    val alertType = givenExistingAlertType(ALERT_TYPE_CODE_VULNERABILITY)
    val alertCode = givenNewAlertCode(alertCode("REA", type = alertType))
    alertCodeRepository.findByCode(alertCode.code)!!.apply {
      deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
      deactivatedBy = "MODIFIED_BY"
      alertCodeRepository.save(this)
    }

    webTestClient.reactivateAlertCode(alertCode.code)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val reactivateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()
    assertThat(reactivateAlertEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_CODE_REACTIVATED.eventType,
        ReferenceDataAdditionalInformation(
          alertCode.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_CODE_REACTIVATED.description,
        reactivateAlertEvent.occurredAt,
        "http://localhost:8080/alert-codes/${alertCode.code}",
      ),
    )
    assertThat(reactivateAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(
      LocalDateTime.now(),
      within(3, ChronoUnit.SECONDS),
    )
  }

  private fun WebTestClient.reactivateAlertCode(alertCode: String): AlertCode =
    patch()
      .uri("/alert-codes/$alertCode/reactivate")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      ).exchange().successResponse()
}
