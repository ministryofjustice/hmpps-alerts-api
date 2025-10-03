package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode as AlertCodeModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType as AlertTypeModel

fun AlertCode.toAlertCodeSummary(username: String) = AlertCodeSummary(
  alertTypeCode = alertType.code,
  alertTypeDescription = alertType.description,
  code = code,
  description = description,
  canBeAdministered = canBeAdministeredByUser(username),
)

fun AlertCode.toAlertCodeModel(username: String) = AlertCodeModel(
  alertTypeCode = alertType.code,
  code = code,
  description = description,
  listSequence = listSequence,
  isActive = isActive(),
  isRestricted = restricted,
  createdAt = createdAt,
  createdBy = createdBy,
  modifiedAt = modifiedAt,
  modifiedBy = modifiedBy,
  deactivatedAt = deactivatedAt,
  deactivatedBy = deactivatedBy,
  canBeAdministered = canBeAdministeredByUser(username),
)

fun Collection<AlertCode>.toAlertCodeModels(username: String, includeInactive: Boolean) = sortedWith(compareBy({ it.listSequence }, { it.code }))
  .filter { includeInactive || it.isActive() }
  .map { it.toAlertCodeModel(username) }

fun AlertType.toAlertTypeModel(username: String, includeInactive: Boolean) = AlertTypeModel(
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
  alertCodes = alertCodes.toAlertCodeModels(username, includeInactive),
)

fun Collection<AlertType>.toAlertTypeModels(username: String, includeInactive: Boolean) = filter { includeInactive || it.isActive() }
  .sortedWith(compareBy({ it.listSequence }, { it.code }))
  .map { it.toAlertTypeModel(username, includeInactive) }

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
  restricted = restricted,
  createdBy = context.username,
  createdAt = context.requestAt,
  listSequence = 0,
)
