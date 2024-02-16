package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping as NomisAlertMappingModel

fun NomisAlertModel.toEntity(objectMapper: ObjectMapper, upsertedAt: LocalDateTime = LocalDateTime.now()) =
  NomisAlert(
    offenderBookId = offenderBookId,
    alertSeq = alertSeq,
    alert = toAlertEntity(),
    nomisAlertData = objectMapper.valueToTree(this),
    upsertedAt = upsertedAt,
  )

fun NomisAlertModel.toAlertEntity() =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = this.alertCode,
    alertType = this.alertType,
    authorisedBy = this.authorizePersonText ?: "",
    offenderId = this.offenderNo,
    validFrom = this.createDate ?: LocalDate.now(),
  )

fun NomisAlert.toMappingModel(status: UpsertStatus) =
  NomisAlertMappingModel(
    offenderBookId = this.offenderBookId,
    alertSeq = this.alertSeq,
    alertUuid = this.alert.alertUuid,
    status = status,
  )
