package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert as BulkAlertModel

fun BulkCreateAlerts.toAlertEntity(
  alertCode: AlertCode,
  prisonNumber: String,
  createdAt: LocalDateTime = LocalDateTime.now(),
  createdBy: String,
  createdByDisplayName: String,
  source: Source,
  activeCaseLoadId: String?,
) = Alert(
  alertCode = alertCode,
  prisonNumber = prisonNumber,
  description = alertCodeDescriptionMap[this.alertCode] ?: this.description,
  authorisedBy = null,
  activeFrom = LocalDate.now(),
  activeTo = null,
  createdAt = createdAt,
  prisonCodeWhenCreated = null,
).create(createdAt = createdAt, createdBy = createdBy, createdByDisplayName = createdByDisplayName, source = source, activeCaseLoadId = activeCaseLoadId)

fun Alert.toBulkAlertAlertModel(message: String = "") =
  BulkAlertAlert(
    alertUuid = id,
    prisonNumber = prisonNumber,
    message = message,
  )

fun BulkCreateAlerts.toEntity(
  objectMapper: ObjectMapper,
  requestedAt: LocalDateTime,
  requestedBy: String,
  requestedByDisplayName: String,
  completedAt: LocalDateTime = LocalDateTime.now(),
  existingActiveAlerts: Collection<BulkAlertAlert>,
  alertsCreated: Collection<BulkAlertAlert>,
  alertsUpdated: Collection<BulkAlertAlert>,
  alertsExpired: Collection<BulkAlertAlert>,
) =
  BulkAlert(
    bulkAlertUuid = UUID.randomUUID(),
    request = objectMapper.valueToTree(this),
    requestedAt = requestedAt,
    requestedBy = requestedBy,
    requestedByDisplayName = requestedByDisplayName,
    completedAt = completedAt,
    successful = true,
    messages = objectMapper.valueToTree(emptyList<String>()),
    existingActiveAlerts = objectMapper.valueToTree(existingActiveAlerts),
    alertsCreated = objectMapper.valueToTree(alertsCreated),
    alertsUpdated = objectMapper.valueToTree(alertsUpdated),
    alertsExpired = objectMapper.valueToTree(alertsExpired),
  )

fun BulkAlert.toModel(objectMapper: ObjectMapper) =
  BulkAlertModel(
    bulkAlertUuid = bulkAlertUuid,
    request = objectMapper.treeToValue<BulkCreateAlerts>(request),
    requestedAt = requestedAt,
    requestedBy = requestedBy,
    requestedByDisplayName = requestedByDisplayName,
    completedAt = completedAt,
    successful = successful,
    messages = objectMapper.treeToValue<List<String>>(messages),
    existingActiveAlerts = objectMapper.treeToValue<List<BulkAlertAlert>>(existingActiveAlerts),
    alertsCreated = objectMapper.treeToValue<List<BulkAlertAlert>>(alertsCreated),
    alertsUpdated = objectMapper.treeToValue<List<BulkAlertAlert>>(alertsUpdated),
    alertsExpired = objectMapper.treeToValue<List<BulkAlertAlert>>(alertsExpired),
  )
