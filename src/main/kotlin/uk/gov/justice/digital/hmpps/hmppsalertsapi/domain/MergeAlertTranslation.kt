package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlert
import java.time.LocalDateTime
import java.util.UUID

fun MergeAlert.toAlertEntity(prisonNumberMergeFrom: String, prisonNumberMergeTo: String, alertCode: AlertCode, mergedAt: LocalDateTime = LocalDateTime.now()) =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = alertCode,
    prisonNumber = prisonNumberMergeTo,
    description = this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom,
    activeTo = this.activeTo,
    createdAt = mergedAt,
  ).also { al ->
    al.auditEvent(
      action = CREATED,
      description = "Merged alert created",
      actionedAt = mergedAt,
      actionedBy = "SYS",
      actionedByDisplayName = "Merge from $prisonNumberMergeFrom",
      source = NOMIS,
      activeCaseLoadId = null,
    )
  }
