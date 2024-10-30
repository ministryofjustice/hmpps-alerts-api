package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AuditEvent as AuditEventModel

const val ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL = "DOCGM"

val alertCodeDescriptionMap = mapOf(
  ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL to """** Offenders must not be made aware of the OCG flag status.  Do not Share with offender. **
    |
    |This person has been mapped as a member of an Organised Crime Group (OCG). If further information is required to assist in management or re-categorisation decisions, including OPT 2 applications please contact the Prison Intelligence Officer.
  """.trimMargin(),
)

fun CreateAlert.toAlertEntity(
  context: AlertRequestContext,
  prisonNumber: String,
  alertCode: AlertCode,
  prisonCode: String?,
) = Alert(
  alertUuid = UUID.randomUUID(),
  alertCode = alertCode,
  prisonNumber = prisonNumber,
  description = alertCodeDescriptionMap[this.alertCode] ?: this.description,
  authorisedBy = this.authorisedBy,
  activeFrom = this.activeFrom ?: LocalDate.now(),
  activeTo = this.activeTo,
  createdAt = context.requestAt,
  prisonCodeWhenCreated = prisonCode,
).create(
  createdAt = context.requestAt,
  createdBy = context.username,
  createdByDisplayName = context.userDisplayName,
  source = context.source,
  activeCaseLoadId = context.activeCaseLoadId,
)

fun Alert.toAlertModel(auditEvents: Collection<AuditEvent>? = null): AlertModel {
  val events = auditEvents ?: auditEvents()
  val createdAuditEvent = events.single { it.action == AuditEventAction.CREATED }
  val lastModifiedAuditEvent = events.firstOrNull { it.action == AuditEventAction.UPDATED }
  val lastActiveToSetAuditEvent = events.takeIf { activeTo != null }
    ?.firstOrNull { it.action == AuditEventAction.UPDATED && it.activeToUpdated == true }

  return AlertModel(
    alertUuid = alertUuid,
    alertCode = alertCode.toAlertCodeSummary(),
    prisonNumber = prisonNumber,
    description = description,
    authorisedBy = authorisedBy,
    activeFrom = activeFrom,
    activeTo = activeTo,
    isActive = isActive(),
    createdAt = createdAuditEvent.actionedAt,
    createdBy = createdAuditEvent.actionedBy,
    createdByDisplayName = createdAuditEvent.actionedByDisplayName,
    lastModifiedAt = lastModifiedAuditEvent?.actionedAt,
    lastModifiedBy = lastModifiedAuditEvent?.actionedBy,
    lastModifiedByDisplayName = lastModifiedAuditEvent?.actionedByDisplayName,
    activeToLastSetAt = lastActiveToSetAuditEvent?.actionedAt,
    activeToLastSetBy = lastActiveToSetAuditEvent?.actionedBy,
    activeToLastSetByDisplayName = lastActiveToSetAuditEvent?.actionedByDisplayName,
  )
}

fun AuditEvent.toAuditEventModel() =
  AuditEventModel(
    action = action,
    description = description,
    actionedAt = actionedAt,
    actionedBy = actionedBy,
    actionedByDisplayName = actionedByDisplayName,
  )
