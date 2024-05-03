package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AlertCodeRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_TYPE_CODE_VULNERABILITY
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReactivateAlertCodeIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var alertCodeRepository: AlertCodeRepository

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
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.patch()
      .uri("/alert-codes/VI/reactivate")
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
      .uri("/alert-codes/VI/reactivate")
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
      .uri("/alert-codes/ALK/reactivate")
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_ADMIN)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Alert with code ALK could not be found")
      assertThat(developerMessage)
        .isEqualTo("Alert with code ALK could not be found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `should mark alert type as active`() {
    val alertCode = createAlertCode()
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
    val alertType = createAlertCode()
    alertCodeRepository.findByCode(alertType.code)!!.apply {
      deactivatedAt = LocalDateTime.of(2010, 3, 7, 16, 27, 58)
      deactivatedBy = "MODIFIED_BY"
      alertCodeRepository.save(this)
    }

    webTestClient.reactivateAlertCode(alertType.code)

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val createAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()
    val reactivateAlertEvent = hmppsEventsQueue.receiveAlertDomainEventOnQueue()

    assertThat(createAlertEvent.eventType).isEqualTo(DomainEventType.ALERT_CODE_CREATED.eventType)
    assertThat(createAlertEvent.additionalInformation.identifier()).isEqualTo(reactivateAlertEvent.additionalInformation.identifier())
    assertThat(reactivateAlertEvent).usingRecursiveComparison().isEqualTo(
      AlertDomainEvent(
        DomainEventType.ALERT_CODE_REACTIVATED.eventType,
        ReferenceDataAdditionalInformation(
          "http://localhost:8080/alert-codes/${alertType.code}",
          alertType.code,
          Source.DPS,
        ),
        1,
        DomainEventType.ALERT_CODE_REACTIVATED.description,
        reactivateAlertEvent.occurredAt,
      ),
    )
    assertThat(reactivateAlertEvent.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(3, ChronoUnit.SECONDS))
  }

  private fun createAlertCodeRequest(
    alertType: String = ALERT_TYPE_CODE_VULNERABILITY,
    alertCode: String = ALERT_CODE_VICTIM,
  ) =
    CreateAlertCodeRequest(alertCode, "description", alertType)

  private fun createAlertCode(): AlertCode {
    val request = createAlertCodeRequest(alertCode = "ABC")
    return webTestClient.post()
      .uri("/alert-codes")
      .bodyValue(request)
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
  }

  private fun WebTestClient.reactivateAlertCode(
    alertCode: String,
  ): AlertCode =
    patch()
      .uri("/alert-codes/$alertCode/reactivate")
      .headers(setAuthorisation(user = TEST_USER, roles = listOf(ROLE_ALERTS_ADMIN), isUserToken = true))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AlertCode::class.java)
      .returnResult().responseBody!!
}
