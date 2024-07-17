package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Comment
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AuditEvent as AuditEventModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Comment as CommentModel

const val ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL = "DOCGM"

val alertCodeDescriptionMap = mapOf(
  ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL to """** Offenders must not be made aware of the OCG flag status.  Do not Share with offender. **
    |
    |This person has been mapped as a member of an Organised Crime Group (OCG). If further information is required to assist in management or re-categorisation decisions, including OPT 2 applications please contact the Prison Intelligence Officer.
  """.trimMargin(),
)

fun CreateAlert.toAlertEntity(
  prisonNumber: String,
  alertCode: AlertCode,
  createdAt: LocalDateTime = LocalDateTime.now(),
  createdBy: String,
  createdByDisplayName: String,
  source: Source,
  activeCaseLoadId: String?,
  prisonCode: String?,
) =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = alertCode,
    prisonNumber = prisonNumber,
    description = alertCodeDescriptionMap[this.alertCode] ?: this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom ?: LocalDate.now(),
    activeTo = this.activeTo,
    createdAt = createdAt,
    prisonCodeWhenCreated = prisonCode,
  ).create(
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    source = source,
    activeCaseLoadId = activeCaseLoadId,
  )

fun Alert.toAlertModel(comments: Collection<Comment>? = null, auditEvents: Collection<AuditEvent>? = null): AlertModel {
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
    comments = (comments ?: comments()).map { it.toAlertCommentModel() },
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

fun Comment.toAlertCommentModel() =
  CommentModel(
    commentUuid = commentUuid,
    comment = comment,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
  )

fun AuditEvent.toAuditEventModel() =
  AuditEventModel(
    action = action,
    description = description,
    actionedAt = actionedAt,
    actionedBy = actionedBy,
    actionedByDisplayName = actionedByDisplayName,
  )
