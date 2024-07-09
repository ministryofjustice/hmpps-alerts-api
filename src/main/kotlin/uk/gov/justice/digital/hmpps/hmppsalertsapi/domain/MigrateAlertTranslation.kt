package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.LanguageFormatUtils
import java.util.UUID

fun MigrateAlert.toAlertEntity(
  prisonNumber: String,
  alertCode: AlertCode,
) =
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
    al.auditEvent(
      action = CREATED,
      description = "Migrated alert created",
      actionedAt = this.createdAt,
      actionedBy = this.createdBy,
      actionedByDisplayName = LanguageFormatUtils.formatDisplayName(this.createdByDisplayName),
      source = NOMIS,
      activeCaseLoadId = null,
    )
    if (this.lastModifiedAt != null) {
      al.lastModifiedAt = this.lastModifiedAt
      al.auditEvent(
        action = UPDATED,
        description = "Migrated alert updated",
        actionedAt = this.lastModifiedAt,
        actionedBy = this.lastModifiedBy!!,
        actionedByDisplayName = LanguageFormatUtils.formatDisplayName(this.lastModifiedByDisplayName!!),
        source = NOMIS,
        activeCaseLoadId = null,
      )
    }
  }
