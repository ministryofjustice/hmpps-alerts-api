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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertTypeRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DeactivateAlertTypeIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var alertTypeRepository: AlertTypeRepository

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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.patch()
      .uri("/alert-types/VI/deactivate")
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
      .uri("/alert-types/VI/deactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
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
  fun `404 alert type not found`() {
    val response = webTestClient.patch()
      .uri("/alert-types/ALK/deactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
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
  fun `should mark alert type as deactivated`() {
    val alertCode = createAlertType()
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
    val request = createAlertTypeRequest("DEF")
    val alertType = createAlertType(request)
    webTestClient.deleteAlertType(alertType.code)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()
    val deleteAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue<ReferenceDataAdditionalInformation>()

    assertThat(createAlertEvent.eventType).isEqualTo(DomainEventType.ALERT_TYPE_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(deleteAlertEvent.additionalInformation.identifier())
    assertThat(deleteAlertEvent).isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_TYPE_DEACTIVATED.eventType,
        ReferenceDataAdditionalInformation(
          "http://localhost:8080/alert-types/${alertType.code}",
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_TYPE_DEACTIVATED.description,
        deleteAlertEvent.occurredAt,
      ),
    )
    assertThat(deleteAlertEvent.occurredAt.toLocalDateTime()).isCloseTo(alertTypeRepository.findByCode(alertType.code)!!.deactivatedAt, within(1, ChronoUnit.MICROS))
  }

  private fun createAlertTypeRequest(code: String = "ABC") =
    CreateAlertTypeRequest(code, "description")

  private fun createAlertType(request: CreateAlertTypeRequest = createAlertTypeRequest()): AlertType {
    return webTestClient.post()
      .uri("/alert-types")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
  }

  private fun WebTestClient.deleteAlertType(
    alertCode: String,
  ): AlertType =
    patch()
      .uri("/alert-types/$alertCode/deactivate")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertType::class.java)
      .returnResult().responseBody!!
}
