package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Comment
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert as AlertModel
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertComment as CommentModel

fun CreateAlert.toAlertEntity(
  alertCode: AlertCode,
  createdAt: LocalDateTime = LocalDateTime.now(),
  createdBy: String,
  createdByDisplayName: String,
) =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = alertCode,
    prisonNumber = this.prisonNumber,
    description = this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom ?: LocalDate.now(),
    activeTo = this.activeTo,
  ).apply {
    auditEvent(
      action = AuditEventAction.CREATED,
      description = "Alert created",
      actionedAt = createdAt,
      actionedBy = createdBy,
      actionedByDisplayName = createdByDisplayName,
    )
  }

fun Alert.toAlertModel() {
  val createdAuditEvent = createdAuditEvent()
  val lastModifiedAuditEvent = lastModifiedAuditEvent()

  AlertModel(
    alertUuid = alertUuid,
    alertCode = alertCode.toAlertCodeSummary(),
    prisonNumber = prisonNumber,
    description = description,
    authorisedBy = authorisedBy,
    activeFrom = activeFrom,
    activeTo = activeTo,
    isActive = isActive(),
    comments = comments().map { it.toAlertCommentModel() },
    createdAt = createdAuditEvent.actionedAt,
    createdBy = createdAuditEvent.actionedBy,
    createdByDisplayName = createdAuditEvent.actionedByDisplayName,
    lastModifiedAt = lastModifiedAuditEvent?.actionedAt,
    lastModifiedBy = lastModifiedAuditEvent?.actionedBy,
    lastModifiedByDisplayName = lastModifiedAuditEvent?.actionedByDisplayName,
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
