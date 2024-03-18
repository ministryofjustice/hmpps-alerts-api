package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode as AlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType as AlertTypeModel

fun AlertCode.toAlertCodeSummary() =
  AlertCodeSummary(
    alertTypeCode = alertType.code,
    alertTypeDescription = alertType.description,
    code = code,
    description = description,
  )

fun AlertCode.toAlertCodeModel() =
  AlertCodeModel(
    alertTypeCode = alertType.code,
    code = code,
    description = description,
    listSequence = listSequence,
    isActive = isActive(),
    createdAt = createdAt,
    createdBy = createdBy,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    deactivatedAt = deactivatedAt,
    deactivatedBy = deactivatedBy,
  )

fun Collection<AlertCode>.toAlertCodeModels() =
  sortedWith(compareBy({ it.listSequence }, { it.code }))
    .map { it.toAlertCodeModel() }

fun AlertType.toAlertTypeModel(includeInactive: Boolean) =
  AlertTypeModel(
    code = code,
    description = description,
    listSequence = listSequence,
    isActive = isActive(),
    createdAt = createdAt,
    createdBy = createdBy,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    deactivatedAt = deactivatedAt,
    deactivatedBy = deactivatedBy,
    alertCodes = alertCodes(includeInactive).toAlertCodeModels(),
  )

fun Collection<AlertType>.toAlertTypeModels(includeInactive: Boolean) =
  filter { includeInactive || it.isActive() }
    .sortedWith(compareBy({ it.listSequence }, { it.code }))
    .map { it.toAlertTypeModel(includeInactive) }
