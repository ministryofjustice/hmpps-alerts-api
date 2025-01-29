package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode as AlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType as AlertTypeModel

fun AlertCode.toAlertCodeSummary() = AlertCodeSummary(
  alertTypeCode = alertType.code,
  alertTypeDescription = alertType.description,
  code = code,
  description = description,
)

fun AlertCode.toAlertCodeModel() = AlertCodeModel(
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

fun Collection<AlertCode>.toAlertCodeModels(includeInactive: Boolean) = sortedWith(compareBy({ it.listSequence }, { it.code }))
  .filter { includeInactive || it.isActive() }
  .map { it.toAlertCodeModel() }

fun AlertType.toAlertTypeModel(includeInactive: Boolean) = AlertTypeModel(
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
  alertCodes = alertCodes.toAlertCodeModels(includeInactive),
)

fun Collection<AlertType>.toAlertTypeModels(includeInactive: Boolean) = filter { includeInactive || it.isActive() }
  .sortedWith(compareBy({ it.listSequence }, { it.code }))
  .map { it.toAlertTypeModel(includeInactive) }

fun CreateAlertTypeRequest.toEntity(context: AlertRequestContext): AlertType = AlertType(
  code = code,
  description = description,
  listSequence = 0,
  createdAt = context.requestAt,
  createdBy = context.username,
)

fun CreateAlertCodeRequest.toEntity(context: AlertRequestContext, alertType: AlertType): AlertCode = AlertCode(
  code = code,
  description = description,
  alertType = alertType,
  createdBy = context.username,
  createdAt = context.requestAt,
  listSequence = 0,
)
