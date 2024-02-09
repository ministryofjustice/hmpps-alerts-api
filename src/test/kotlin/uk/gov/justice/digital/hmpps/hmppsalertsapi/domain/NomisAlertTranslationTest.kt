package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisAlertStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.NomisCaseloadType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert as AlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert as NomisAlertEntity
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping as NomisAlertMappingModel

class NomisAlertTranslationTest {
  private val objectMapper = jacksonMapperBuilder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .addModule(JavaTimeModule())
    .build()

  val nomisAlertModel = NomisAlertModel(
    alertDate = LocalDate.of(2023, 11, 27),
    offenderBookId = 3,
    alertSeq = 1,
    rootOffenderId = 2,
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
    auditTimestamp = LocalDateTime.of(2023, 3, 6, 14, 30),
    auditUserId = "CREATED_OR_MODIFIED_BY",
    auditModuleName = "AUDIT_MODULE",
    auditClientUserId = "CREATED_OR_MODIFIED_BY",
    auditClientIpAddress = "1.2.3.4",
    auditClientWorkstationName = "WORKSTATION_NAME",
    auditAdditionalInfo = "PUT /api/bookings/12345/alert/1",
  )

  @Test
  fun `should convert NomisAlert model to NomisAlert entity`() {
    val alertUuid = UUID.randomUUID()
    val upsertedAt = LocalDateTime.now()

    val expectedEntity = NomisAlertEntity(
      nomisAlertId = 0,
      offenderBookId = 3,
      alertSeq = 1,
      alertUuid = alertUuid,
      nomisAlertData = objectMapper.valueToTree(nomisAlertModel),
      upsertedAt = upsertedAt,
    )

    val entity = nomisAlertModel.toEntity(objectMapper, alertUuid, upsertedAt)

    assertThat(entity).isEqualTo(expectedEntity)
    assertThat(entity.removedAt).isNull()
  }

  @Test
  fun `should convert NomisAlert model to Alert entity`() {
    val alertUuid = UUID.randomUUID()

    val expectedEntity = AlertEntity(
      alertUuid = alertUuid,
    )

    val entity = nomisAlertModel.toAlertEntity(alertUuid)

    assertThat(entity).isEqualTo(expectedEntity)
  }

  @Test
  fun `should convert NomisAlert entity to NomisAlertMapping model`() {
    val alertUuid = UUID.randomUUID()
    val upsertedAt = LocalDateTime.now()

    val expectedModel = NomisAlertMappingModel(
      offenderBookId = 3,
      alertSeq = 1,
      alertUuid = alertUuid,
    )

    val entity = NomisAlertEntity(
      nomisAlertId = 0,
      offenderBookId = 3,
      alertSeq = 1,
      alertUuid = alertUuid,
      nomisAlertData = objectMapper.valueToTree(nomisAlertModel),
      upsertedAt = upsertedAt,
    )

    val model = entity.toMappingModel()

    assertThat(model).isEqualTo(expectedModel)
  }
}