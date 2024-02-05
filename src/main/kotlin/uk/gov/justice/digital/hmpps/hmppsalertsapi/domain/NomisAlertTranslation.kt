package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.NomisAlert
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert as NomisAlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping as NomisAlertMappingModel

fun NomisAlertModel.toEntity(objectMapper: ObjectMapper, alertUuid: UUID) =
  NomisAlert(
    offenderBookId = offenderBookId,
    alertSeq = alertSeq,
    alertUuid = alertUuid,
    nomisAlertData = objectMapper.valueToTree(this),
  )

fun NomisAlertModel.toAlertEntity(alertUuid: UUID) =
  Alert(
    alertUuid = alertUuid,
  )

fun NomisAlert.toMappingModel() =
  NomisAlertMappingModel(
    offenderBookId = this.offenderBookId,
    alertSeq = this.alertSeq,
    alertUuid = this.alertUuid,
  )
