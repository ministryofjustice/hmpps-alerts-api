package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisAlertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisCaseloadType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.NomisAlertRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping as NomisAlertMappingModel

class UpsertNomisAlertIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var nomisAlertRepository: NomisAlertRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/nomis-alerts")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/nomis-alerts")
      .bodyValue(nomisAlertModel())
      .headers(setAuthorisation())
      .headers(setSyncContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts reader`() {
    webTestClient.post()
      .uri("/nomis-alerts")
      .bodyValue(nomisAlertModel())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_READER)))
      .headers(setSyncContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - alerts writer`() {
    webTestClient.post()
      .uri("/nomis-alerts")
      .bodyValue(nomisAlertModel())
      .headers(setAuthorisation(roles = listOf(ROLE_ALERTS_WRITER)))
      .headers(setSyncContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/nomis-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setSyncContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public org.springframework.http.ResponseEntity<uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping> uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.NomisAlertsController.upsertNomisAlert(uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `405 method not allowed`() {
    val response = webTestClient.put()
      .uri("/nomis-alerts")
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setSyncContext())
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(405)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Method not allowed failure: Request method 'PUT' is not supported")
      assertThat(developerMessage).isEqualTo("Request method 'PUT' is not supported")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create response contains mapping ids and status`() {
    val nomisAlertModel = nomisAlertModel()
    val nomisAlertMapping = webTestClient.upsertNomisAlert(nomisAlertModel, expectedStatusCode = HttpStatus.CREATED)

    assertThat(nomisAlertMapping.offenderBookId).isEqualTo(nomisAlertModel.offenderBookId)
    assertThat(nomisAlertMapping.alertSeq).isEqualTo(nomisAlertModel.alertSeq)
    assertThat(nomisAlertMapping.alertUuid).isInstanceOf(UUID::class.java)
    assertThat(nomisAlertMapping.alertUuid).isNotEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    assertThat(nomisAlertMapping.status).isEqualTo(UpsertStatus.CREATED)
  }

  @Test
  fun `new NOMIS alert is created`() {
    val nomisAlertModel = nomisAlertModel()
    val nomisAlertMapping = webTestClient.upsertNomisAlert(nomisAlertModel, expectedStatusCode = HttpStatus.CREATED)

    val nomisAlertEntity = nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)

    assertThat(nomisAlertEntity).isNotNull
    assertThat(nomisAlertEntity!!.nomisAlertId).isGreaterThan(0)
    assertThat(nomisAlertEntity.offenderBookId).isEqualTo(nomisAlertModel.offenderBookId)
    assertThat(nomisAlertEntity.alertSeq).isEqualTo(nomisAlertModel.alertSeq)
    assertThat(nomisAlertEntity.alertUuid).isEqualTo(nomisAlertMapping.alertUuid)
    assertThat(objectMapper.treeToValue(nomisAlertEntity.nomisAlertData, NomisAlertModel::class.java)).isEqualTo(nomisAlertModel)
    assertThat(nomisAlertEntity.upsertedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
    assertThat(nomisAlertEntity.removedAt).isNull()
  }

  @Sql("classpath:test_data/nomis-alert-offender-book-id-3-alert-seq-1.sql")
  @Test
  fun `update response contains mapping ids and status`() {
    val offenderBookId = 3L
    val alertSeq = 1
    val nomisAlertModel = nomisAlertModel(offenderBookId, alertSeq)
    val nomisAlertMapping = webTestClient.upsertNomisAlert(nomisAlertModel, expectedStatusCode = HttpStatus.OK)

    assertThat(nomisAlertMapping.offenderBookId).isEqualTo(3L)
    assertThat(nomisAlertMapping.alertSeq).isEqualTo(1)
    assertThat(nomisAlertMapping.alertUuid).isEqualTo(UUID.fromString("bf0da40f-5a8d-4630-94dd-a7412f007023"))
    assertThat(nomisAlertMapping.status).isEqualTo(UpsertStatus.UPDATED)
  }

  @Sql("classpath:test_data/nomis-alert-offender-book-id-3-alert-seq-1.sql")
  @Test
  fun `existing NOMIS alert is updated`() {
    val offenderBookId = 3L
    val alertSeq = 1
    val nomisAlertModel = nomisAlertModel(offenderBookId, alertSeq)
    webTestClient.upsertNomisAlert(nomisAlertModel, expectedStatusCode = HttpStatus.OK)

    val nomisAlertEntity = nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)

    assertThat(nomisAlertEntity).isNotNull
    assertThat(nomisAlertEntity!!.nomisAlertId).isEqualTo(1)
    assertThat(nomisAlertEntity.offenderBookId).isEqualTo(offenderBookId)
    assertThat(nomisAlertEntity.alertSeq).isEqualTo(alertSeq)
    assertThat(nomisAlertEntity.alertUuid).isEqualTo(UUID.fromString("bf0da40f-5a8d-4630-94dd-a7412f007023"))
    assertThat(objectMapper.treeToValue(nomisAlertEntity.nomisAlertData, NomisAlertModel::class.java)).isEqualTo(nomisAlertModel)
    assertThat(nomisAlertEntity.upsertedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
    assertThat(nomisAlertEntity.removedAt).isNull()
  }

  private fun nomisAlertModel(
    offenderBookId: Long = 3,
    alertSeq: Int = 1,
  ) =
    NomisAlertModel(
      alertDate = LocalDate.of(2023, 11, 27),
      offenderBookId = offenderBookId,
      rootOffenderId = 2,
      alertSeq = alertSeq,
      offenderNo = PRISON_NUMBER,
      alertType = "A",
      alertCode = "ABC",
      authorizePersonText = "A. Authorizer",
      createDate = LocalDate.of(2022, 9, 15),
      alertStatus = NomisAlertStatus.ACTIVE.name,
      verifiedFlag = false,
      verifiedDatetime = null,
      verifiedUserId = null,
      expiryDate = null,
      commentText = "Alert comment",
      // Always null in NOMIS
      caseloadId = null,
      modifyUserId = "MODIFIED_BY",
      modifyDatetime = LocalDateTime.of(2023, 3, 6, 14, 30),
      caseloadType = NomisCaseloadType.INST.name,
      createDatetime = LocalDateTime.of(2022, 9, 15, 9, 25),
      createUserId = "CREATED_BY",
      // In discussion about removing the following properties as likely not needed
      auditTimestamp = LocalDateTime.of(2023, 3, 6, 14, 30),
      auditUserId = "CREATED_OR_MODIFIED_BY",
      auditModuleName = "AUDIT_MODULE",
      auditClientUserId = "CREATED_OR_MODIFIED_BY",
      auditClientIpAddress = "1.2.3.4",
      auditClientWorkstationName = "WORKSTATION_NAME",
      auditAdditionalInfo = "PUT /api/bookings/12345/alert/1",
    )

  private fun WebTestClient.upsertNomisAlert(
    nomisAlert: NomisAlertModel,
    suppressEvents: Boolean = false,
    expectedStatusCode: HttpStatusCode,
  ) =
    post()
      .uri("/nomis-alerts")
      .bodyValue(nomisAlert)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS_ALERTS)))
      .headers(setSyncContext(suppressEvents))
      .exchange()
      .expectStatus().isEqualTo(expectedStatusCode)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(NomisAlertMappingModel::class.java)
      .returnResult().responseBody!!
}
