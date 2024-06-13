package uk.gov.justice.digital.hmpps.hmppsalertsapi.domain

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason.MERGE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlert
import java.time.LocalDateTime
import java.util.UUID

fun MergeAlert.toAlertEntity(prisonNumberMergeFrom: String, prisonNumberMergeTo: String, alertCode: AlertCode, mergedAt: LocalDateTime, publishEvent: Boolean) =
  Alert(
    alertUuid = UUID.randomUUID(),
    alertCode = alertCode,
    prisonNumber = prisonNumberMergeTo,
    description = this.description,
    authorisedBy = this.authorisedBy,
    activeFrom = this.activeFrom,
    activeTo = this.activeTo,
    createdAt = mergedAt,
  ).create(
    description = "Alert created when merging alerts from prison number '$prisonNumberMergeFrom' into prison number '$prisonNumberMergeTo'",
    createdAt = mergedAt,
    createdBy = "SYS",
    createdByDisplayName = "Merge from $prisonNumberMergeFrom",
    source = NOMIS,
    reason = MERGE,
    activeCaseLoadId = null,
    publishEvent = publishEvent,
  )
