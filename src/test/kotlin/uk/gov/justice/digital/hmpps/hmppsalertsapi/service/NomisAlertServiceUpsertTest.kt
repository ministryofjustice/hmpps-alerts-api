package uk.gov.justice.digital.hmpps.hmppsalertsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.SyncContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.toEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisAlertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisCaseloadType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.NomisAlertRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.testObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel

class NomisAlertServiceUpsertTest {
  private val nomisAlertRepository: NomisAlertRepository = mock()
  private val objectMapper = testObjectMapper()

  private val service = NomisAlertService(nomisAlertRepository, objectMapper)

  private val syncContext = SyncContext(false)
  private val nomisAlertCaptor = argumentCaptor<NomisAlert>()

  @Test
  fun `saves new NOMIS alert`() {
    val nomisAlertModel = nomisAlertModel()

    whenever(nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)).thenReturn(null)

    service.upsertNomisAlert(nomisAlertModel, syncContext)

    verify(nomisAlertRepository).saveAndFlush(any<NomisAlert>())
  }

  @Test
  fun `new NOMIS alert uses to entity transformation to convert model to entity`() {
    val nomisAlertModel = nomisAlertModel()

    whenever(nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)).thenReturn(null)
    whenever(nomisAlertRepository.saveAndFlush(nomisAlertCaptor.capture())).thenAnswer { nomisAlertCaptor.firstValue }

    service.upsertNomisAlert(nomisAlertModel, syncContext)

    nomisAlertCaptor.firstValue.run {
      assertThat(this).isEqualTo(nomisAlertModel.toEntity(objectMapper, this.alertUuid, this.upsertedAt))
    }
  }

  @Test
  fun `returns mapping info for new NOMIS alert`() {
    val nomisAlertModel = nomisAlertModel()

    whenever(nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)).thenReturn(null)

    val result = service.upsertNomisAlert(nomisAlertModel, syncContext)

    assertThat(result).isEqualTo(
      NomisAlertMapping(
        offenderBookId = nomisAlertModel.offenderBookId,
        alertSeq = nomisAlertModel.alertSeq,
        alertUuid = result.alertUuid,
        status = UpsertStatus.CREATED,
      ),
    )
    assertThat(result.alertUuid).isNotEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"))
  }

  @Test
  fun `updates existing NOMIS alert`() {
    val nomisAlertModel = nomisAlertModel()
    val existingNomisAlert = NomisAlert(
      offenderBookId = nomisAlertModel.offenderBookId,
      alertSeq = nomisAlertModel.alertSeq,
      alertUuid = UUID.randomUUID(),
      nomisAlertData = objectMapper.valueToTree(nomisAlertModel(commentText = "Original comment")),
      upsertedAt = LocalDateTime.now().minusDays(1),
    )

    whenever(nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)).thenReturn(existingNomisAlert)

    service.upsertNomisAlert(nomisAlertModel, syncContext)

    verify(nomisAlertRepository).saveAndFlush(any<NomisAlert>())

    with(existingNomisAlert) {
      assertThat(nomisAlertData["commentText"].asText()).isNotEqualTo("Original comment")
      assertThat(nomisAlertData["commentText"].asText()).isEqualTo(nomisAlertModel.commentText)
      assertThat(upsertedAt).isCloseToUtcNow(within(3, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `returns mapping info for updated NOMIS alert`() {
    val offenderBookId = 3L
    val alertSeq = 1
    val nomisAlertModel = nomisAlertModel(offenderBookId, alertSeq)
    val existingNomisAlert = NomisAlert(
      offenderBookId = nomisAlertModel.offenderBookId,
      alertSeq = nomisAlertModel.alertSeq,
      alertUuid = UUID.randomUUID(),
      nomisAlertData = objectMapper.valueToTree(nomisAlertModel(commentText = "Original comment")),
      upsertedAt = LocalDateTime.now().minusDays(1),
    )

    whenever(nomisAlertRepository.findByOffenderBookIdAndAlertSeq(nomisAlertModel.offenderBookId, nomisAlertModel.alertSeq)).thenReturn(existingNomisAlert)

    val result = service.upsertNomisAlert(nomisAlertModel, syncContext)

    assertThat(result).isEqualTo(
      NomisAlertMapping(
        offenderBookId = offenderBookId,
        alertSeq = alertSeq,
        alertUuid = existingNomisAlert.alertUuid,
        status = UpsertStatus.UPDATED,
      ),
    )
  }

  private fun nomisAlertModel(
    offenderBookId: Long = 3,
    alertSeq: Int = 1,
    commentText: String = "Alert comment",
  ) =
    NomisAlertModel(
      alertDate = LocalDate.of(2023, 11, 27),
      offenderBookId = offenderBookId,
      rootOffenderId = 2,
      alertSeq = alertSeq,
      offenderNo = "A1234AA",
      alertType = "A",
      alertCode = "ABC",
      authorizePersonText = "A. Authorizer",
      createDate = LocalDate.of(2022, 9, 15),
      alertStatus = NomisAlertStatus.ACTIVE.name,
      verifiedFlag = false,
      verifiedDatetime = null,
      verifiedUserId = null,
      expiryDate = null,
      commentText = commentText,
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
}
