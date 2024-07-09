package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.temporal.ChronoUnit

class UpdateAlertTypeIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch()
      .uri("/alert-types/VI")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.patch()
      .uri("/alert-types/VI")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertTypeRequest("description"))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.patch()
      .uri("/alert-types/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertTypeRequest("description"))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.patch()
      .uri("/alert-types/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
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
    val response = webTestClient.patch()
      .uri("/alert-types/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(username = USER_NOT_FOUND))
      .bodyValue(UpdateAlertTypeRequest("description"))
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
  fun `404 alert type not found`() {
    val response = webTestClient.patch()
      .uri("/alert-types/ALK")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertTypeRequest("description"))
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert type not found")
      assertThat(developerMessage).isEqualTo("Alert type not found with identifier ALK")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - empty description`() {
    val alertTypeCode = "EMPTY"
    val response = webTestClient.patch()
      .uri("/alert-types/$alertTypeCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI), isUserToken = true))
      .bodyValue(UpdateAlertTypeRequest(""))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).contains("Description must be between 1 & 40 characters")
      assertThat(developerMessage).contains("Description must be between 1 & 40 characters")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - description too long`() {
    val alertTypeCode = "TLDR"
    val response = webTestClient.patch()
      .uri("/alert-types/$alertTypeCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI), isUserToken = true))
      .bodyValue(UpdateAlertTypeRequest("descdescdescdescdescdescdescdescdescdescd"))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).contains("Description must be between 1 & 40 characters")
      assertThat(developerMessage).contains("Description must be between 1 & 40 characters")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should update alert type description`() {
    val alertType = givenNewAlertType(alertType("NDV"))
    val response = webTestClient.updateAlertTypeDescription(alertType.code, "New Description Value")
    assertThat(response.description).isEqualTo("New Description Value")
  }

  @Test
  fun `should publish alert types updated event with NOMIS source`() {
    val alertType = givenNewAlertType(alertType("NDE"))
    webTestClient.updateAlertTypeDescription(alertType.code, "New Description Value")

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val updateAlertTypeEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()
    assertThat(updateAlertTypeEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_UPDATED.eventType,
        ReferenceDataAdditionalInformation(
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_UPDATED.description,
        updateAlertTypeEvent.occurredAt,
        "http://localhost:8080/alert-types/${alertType.code}",
      ),
    )
    assertThat(updateAlertTypeEvent.occurredAt.toLocalDateTime()).isCloseTo(
      alertTypeRepository.findByCode(alertType.code)!!.modifiedAt,
      within(1, ChronoUnit.MICROS),
    )
  }

  private fun WebTestClient.updateAlertTypeDescription(alertCode: String, description: String): AlertType =
    patch()
      .uri("/alert-types/$alertCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI), isUserToken = true))
      .bodyValue(UpdateAlertTypeRequest(description))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
}
