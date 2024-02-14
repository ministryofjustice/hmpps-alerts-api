package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping as NomisAlertMappingModel

fun NomisAlertModel.toEntity(objectMapper: ObjectMapper, alertUuid: UUID, upsertedAt: LocalDateTime = LocalDateTime.now()) =
  NomisAlert(
    offenderBookId = offenderBookId,
    alertSeq = alertSeq,
    alertUuid = alertUuid,
    nomisAlertData = objectMapper.valueToTree(this),
    upsertedAt = upsertedAt,
  )

fun NomisAlertModel.toAlertEntity(alertUuid: UUID) =
  Alert(
    alertUuid = alertUuid,
  )

fun NomisAlert.toMappingModel(status: UpsertStatus) =
  NomisAlertMappingModel(
    offenderBookId = this.offenderBookId,
    alertSeq = this.alertSeq,
    alertUuid = this.alertUuid,
    status = status,
  )
