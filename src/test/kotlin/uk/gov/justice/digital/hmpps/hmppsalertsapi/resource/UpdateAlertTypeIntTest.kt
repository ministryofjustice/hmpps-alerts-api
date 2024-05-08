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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertTypeRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertTypeRequest("description"))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.patch()
      .uri("/alert-types/VI")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .bodyValue(UpdateAlertTypeRequest("description"))
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert type with code ALK could not be found")
      assertThat(developerMessage)
        .isEqualTo("Alert type with code ALK could not be found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - empty description`() {
    val alertTypeCode = createAlertType().code
    val response = webTestClient.patch()
      .uri("/alert-types/$alertTypeCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
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
    val alertTypeCode = createAlertType().code
    val response = webTestClient.patch()
      .uri("/alert-types/$alertTypeCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
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
    val alertType = createAlertType()
    val response = webTestClient.updateAlertTypeDescription(alertType.code, "New Description Value")
    assertThat(response.description).isEqualTo("New Description Value")
  }

  @Test
  fun `should publish alert types updated event with NOMIS source`() {
    val alertType = createAlertType()
    webTestClient.updateAlertTypeDescription(alertType.code, "New Description Value")

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()
    val updateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(createAlertEvent.eventType).isEqualTo(DomainEventType.ALERT_TYPE_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(updateAlertEvent.additionalInformation.identifier())
    assertThat(updateAlertEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_UPDATED.eventType,
        ReferenceDataAdditionalInformation(
          "http://localhost:8080/alert-types/${alertType.code}",
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_UPDATED.description,
        updateAlertEvent.occurredAt,
      ),
    )
    assertThat(OffsetDateTime.parse(updateAlertEvent.occurredAt).toLocalDateTime()).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
  }

  private fun createAlertType(): AlertType {
    return webTestClient.post()
      .uri("/alert-types")
      .bodyValue(CreateAlertTypeRequest("ABC", "description"))
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
  }

  private fun WebTestClient.updateAlertTypeDescription(
    alertCode: String,
    description: String,
  ): AlertType =
    patch()
      .uri("/alert-types/$alertCode")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .bodyValue(UpdateAlertTypeRequest(description))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
}
