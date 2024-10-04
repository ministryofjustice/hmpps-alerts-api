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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_VULNERABILITY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import java.time.temporal.ChronoUnit

class UpdateAlertCodeIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch()
      .uri("/alert-codes/VI")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.patch()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertCodeRequest("description"))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.patch()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertCodeRequest("description"))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.patch()
      .uri("/alert-codes/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .bodyValue(UpdateAlertCodeRequest("description"))
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
  fun `404 alert code not found`() {
    val response = webTestClient.patch()
      .uri("/alert-codes/ALK")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertCodeRequest("description"))
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
  fun `400 bad request - empty description`() {
    val alertCode = "EMPTY"
    val response = webTestClient.patch()
      .uri("/alert-codes/$alertCode")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .bodyValue(UpdateAlertCodeRequest(""))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).contains("Description must be between 1 & 40 characters")
      assertThat(developerMessage).contains("Description must be between 1 & 40 characters")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - description too long`() {
    val alertCode = "TLDR"

    val response = webTestClient.patch()
      .uri("/alert-codes/$alertCode")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .bodyValue(UpdateAlertTypeRequest("descdescdescdescdescdescdescdescdescdescd"))
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).contains("Description must be between 1 & 40 characters")
      assertThat(developerMessage).contains("Description must be between 1 & 40 characters")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should update alert code description`() {
    val alertType = givenExistingAlertType(ALERT_TYPE_CODE_VULNERABILITY)
    val alertCode = givenNewAlertCode(alertCode("UPD", type = alertType))
    val response = webTestClient.updateAlertCodeDescription(alertCode.code, "New Description Value")
    assertThat(response.description).isEqualTo("New Description Value")
  }

  @Test
  fun `should publish alert code updated event with NOMIS source`() {
    val alertType = givenExistingAlertType(ALERT_TYPE_CODE_VULNERABILITY)
    val alertCode = givenNewAlertCode(alertCode("UPN", type = alertType))

    webTestClient.updateAlertCodeDescription(alertCode.code, "New Description Value")

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val updateAlertCodeEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()
    assertThat(updateAlertCodeEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_CODE_UPDATED.eventType,
        ReferenceDataAdditionalInformation(
          alertCode.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_CODE_UPDATED.description,
        updateAlertCodeEvent.occurredAt,
        "http://localhost:8080/alert-codes/${alertCode.code}",
      ),
    )
    assertThat(updateAlertCodeEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertCodeRepository.findByCode(alertCode.code)!!.modifiedAt,
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun WebTestClient.updateAlertCodeDescription(alertCode: String, description: String): AlertCode =
    patch()
      .uri("/alert-codes/$alertCode")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .bodyValue(UpdateAlertCodeRequest(description))
      .exchange().successResponse()
}
