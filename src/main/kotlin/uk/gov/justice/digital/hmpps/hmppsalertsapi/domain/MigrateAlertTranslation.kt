package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import java.time.LocalDateTime
import java.util.UUID

fun MigrateAlert.toAlertEntity(prisonNumber: String, alertCode: AlertCode, migratedAt: LocalDateTime = LocalDateTime.now()) =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = alertCode,
    prisonNumber = prisonNumber,
    description = this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom,
    activeTo = this.activeTo,
    createdAt = this.createdAt,
  ).also { al ->
    al.migratedAlert = MigratedAlert(
      offenderBookId = offenderBookId,
      bookingSeq = bookingSeq,
      alertSeq = alertSeq,
      alert = al,
      migratedAt = migratedAt,
    )
    al.auditEvent(
      action = CREATED,
      description = "Migrated alert created",
      actionedAt = this.createdAt,
      actionedBy = this.createdBy,
      actionedByDisplayName = this.createdByDisplayName,
      source = NOMIS,
      activeCaseLoadId = null,
    )
    if (this.updatedAt != null) {
      al.lastModifiedAt = this.updatedAt
      al.auditEvent(
        action = UPDATED,
        description = "Migrated alert updated",
        actionedAt = this.updatedAt,
        actionedBy = this.updatedBy!!,
        actionedByDisplayName = this.updatedByDisplayName!!,
        source = NOMIS,
        activeCaseLoadId = null,
      )
    }
  }
