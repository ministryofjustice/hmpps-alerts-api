package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.USER_THROW_EXCEPTION
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.BulkAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.RequestGenerator.bulkAlertRequest
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert as BulkAlertModel

class PlanBulkAlertsIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var bulkAlertRepository: BulkAlertRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts/plan")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.planBulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  companion object {
    @JvmStatic
    fun badRequestParameters(): List<Arguments> = listOf(
      Arguments.of(
        bulkAlertRequest(),
        "At least one prison number must be supplied",
        "prison numbers required",
      ),
      Arguments.of(
        bulkAlertRequest(PRISON_NUMBER, alertCode = ""),
        "Alert code must be supplied and be <= 12 characters",
        "alert code required",
      ),
      Arguments.of(
        bulkAlertRequest(PRISON_NUMBER, alertCode = 'a'.toString().repeat(13)),
        "Alert code must be supplied and be <= 12 characters",
        "alert code greater than 12 characters",
      ),
    )
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("badRequestParameters")
  fun `400 bad request - property validation`(
    request: BulkCreateAlerts,
    expectedUserMessage: String,
    displayName: String,
  ) {
    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): $expectedUserMessage")
      assertThat(developerMessage).contains(expectedUserMessage)
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple property errors`() {
    val request = bulkAlertRequest(alertCode = "")

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failure(s): Alert code must be supplied and be <= 12 characters
          |At least one prison number must be supplied
        """.trimMargin(),
      )
      assertThat(developerMessage).startsWith("Validation failed for argument [0] in public uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.BulkAlertsController.planBulkCreateAlerts(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts,jakarta.servlet.http.HttpServletRequest) with 2 errors:")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert codes not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER, alertCode = "NOT_FOUND")

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is invalid")
      assertThat(developerMessage).isEqualTo("Details => Alert code:NOT_FOUND")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - alert code is inactive`() {
    val request = bulkAlertRequest(PRISON_NUMBER, alertCode = ALERT_CODE_INACTIVE_COVID_REFUSING_TO_SHIELD)

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Alert code is inactive")
      assertThat(developerMessage).isEqualTo("Alert code is inactive")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = bulkAlertRequest(PRISON_NUMBER_NOT_FOUND)

    val response = webTestClient.planBulkCreateAlertResponseSpec(request = request).errorResponse(BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prison number(s) not found")
      assertThat(developerMessage).isEqualTo("Prison number(s) not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.patch()
      .uri("/bulk-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange()
      .errorResponse(HttpStatus.METHOD_NOT_ALLOWED)

    with(response) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'PATCH' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PATCH' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `502 bad gateway - get user details request failed`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER))
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(username = USER_THROW_EXCEPTION))
      .exchange()
      .errorResponse(HttpStatus.BAD_GATEWAY)

    with(response) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get user details request failed")
      assertThat(developerMessage).isEqualTo("Get user details request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `502 bad gateway - get prisoner request failed`() {
    val response = webTestClient.post()
      .uri("/bulk-alerts")
      .bodyValue(bulkAlertRequest(PRISON_NUMBER_THROW_EXCEPTION))
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange()
      .errorResponse(HttpStatus.BAD_GATEWAY)

    with(response) {
      assertThat(status).isEqualTo(502)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Downstream service exception: Get prisoner request failed")
      assertThat(developerMessage).isEqualTo("Get prisoner request failed")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `does not create new alert`() {
    val prisonNumber = "B1234LK"
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val response = webTestClient.planBulkCreateAlert(request)

    val createdAlert = response.alertsCreated.single()
    assertThat(alertRepository.findByIdOrNull(createdAlert.alertUuid)).isNull()
  }

  @Test
  fun `does not store bulk alert`() {
    val prisonNumber = "B1235LK"
    givenPrisonersExist(prisonNumber)

    val request = bulkAlertRequest(prisonNumber)
    val response = webTestClient.planBulkCreateAlert(request)

    assertThat(bulkAlertRepository.findByBulkAlertUuid(response.bulkAlertUuid)).isNull()
  }

  @Test
  fun `returns list of new alerts to be created but does not create them`() {
    val prisonNumber = "B1237LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAlert(alert(prisonNumber, alertCode))

    val request = bulkAlertRequest(prisonNumber)

    val response = webTestClient.planBulkCreateAlert(request)

    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.existingActiveAlerts.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.id)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEmpty()
    }

    verify(hmppsQueueService, timeout(1000).times(0)).findByTopicId(any())
  }

  @Test
  fun `returns list of alerts to be updated but do not update them`() {
    val prisonNumber = "B1238LK"
    givenPrisonersExist(prisonNumber)
    val alertCode = givenExistingAlertCode(ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL)
    val existingAlert = givenAlert(alert(prisonNumber, alertCode, activeTo = LocalDate.now().plusDays(1)))

    val request = bulkAlertRequest(prisonNumber)
    val response = webTestClient.planBulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsCreated).isEmpty()
    assertThat(response.alertsExpired).isEmpty()
    with(response.alertsUpdated.single()) {
      assertThat(alertUuid).isEqualTo(existingAlert.id)
      assertThat(prisonNumber).isEqualTo(existingAlert.prisonNumber)
      assertThat(message).isEqualTo("Updated active to from '${existingAlert.activeTo}' to 'null'")
      with(alertRepository.findByIdOrNull(alertUuid)!!) {
        assertThat(isActive()).isTrue()
        assertThat(activeTo).isNotNull()
      }
    }
  }

  @Test
  fun `cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED returns list of alert to be deactivated but does not update them`() {
    val prisonNumber = prisonNumber()
    givenPrisonersExist(prisonNumber)
    val request = bulkAlertRequest(
      prisonNumber,
      cleanupMode = EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED,
    )

    val prisonNumbersToExpire = setOf(prisonNumber(), prisonNumber())
    val toExpire = prisonNumbersToExpire.map { givenAlert(alert(it, givenExistingAlertCode(request.alertCode))) }

    val response = webTestClient.planBulkCreateAlert(request)

    assertThat(response.existingActiveAlerts).isEmpty()
    assertThat(response.alertsUpdated).isEmpty()
    with(response.alertsCreated.single()) {
      assertThat(this.prisonNumber).isEqualTo(prisonNumber)
      assertThat(message).isEmpty()
      assertThat(alertRepository.findByIdOrNull(alertUuid)).isNull()
    }
    with(response.alertsExpired) {
      assertThat(this).hasSizeGreaterThanOrEqualTo(2)
      assertThat(map { it.alertUuid }).containsAll(toExpire.map { it.id })
      assertThat(map { it.prisonNumber }).containsAll(prisonNumbersToExpire)
      onEach {
        with(alertRepository.findByIdOrNull(it.alertUuid)!!) {
          assertThat(isActive()).isTrue()
        }
      }
    }
  }

  private fun WebTestClient.planBulkCreateAlertResponseSpec(request: BulkCreateAlerts) =
    post()
      .uri("/bulk-alerts/plan")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext(source = DPS))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.planBulkCreateAlert(request: BulkCreateAlerts) =
    planBulkCreateAlertResponseSpec(request)
      .expectStatus().isOk
      .expectBody<BulkAlertModel>()
      .returnResult().responseBody!!
}
