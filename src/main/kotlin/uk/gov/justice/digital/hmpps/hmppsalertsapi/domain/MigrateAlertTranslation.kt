package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlertRequest
import java.time.LocalDateTime
import java.util.UUID

fun MigrateAlertRequest.toAlertEntity(alertCode: AlertCode): Alert {
  val alert = Alert(
    alertCode = alertCode,
    prisonNumber = this.prisonNumber,
    description = this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom,
    activeTo = this.activeTo,
    alertUuid = UUID.randomUUID(),
    migratedAt = LocalDateTime.now(),
  )
  alert.also { al: Alert ->
    al.auditEvent(
      action = CREATED,
      description = "Migrated alert created",
      actionedAt = this.createdAt,
      actionedBy = this.createdBy,
      actionedByDisplayName = this.createdByDisplayName,
    )
    comments.forEach { al.addComment(it.comment, it.createdAt, it.createdBy, it.createdByDisplayName) }
    if (this.updatedAt != null) {
      al.auditEvent(
        action = UPDATED,
        description = "Migrated alert updated",
        actionedAt = this.updatedAt,
        actionedBy = this.updatedBy!!,
        actionedByDisplayName = this.updatedByDisplayName!!,
      )
    }
  }
  return alert
}