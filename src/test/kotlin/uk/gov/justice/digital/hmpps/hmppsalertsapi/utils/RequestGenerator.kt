package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.domain.ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode.ADD_MISSING
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AC_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AT_VULNERABILITY
import java.time.LocalDate
import java.time.LocalDateTime

object RequestGenerator {
  fun AlertCode.summary() = AlertCodeSummary(alertType.code, alertType.description, code, description)

  fun alertCodeSummary(alertType: AlertType = AT_VULNERABILITY, alertCode: AlertCode = AC_VICTIM) =
    AlertCodeSummary(alertType.code, alertType.description, alertCode.code, alertCode.description)

  fun bulkAlertRequest(
    vararg prisonNumbers: String,
    alertCode: String = ALERT_CODE_SECURITY_ALERT_OCG_NOMINAL,
    mode: BulkCreateAlertMode = ADD_MISSING,
    cleanupMode: BulkCreateAlertCleanupMode = KEEP_ALL,
  ) = BulkCreateAlerts(prisonNumbers.asList(), alertCode, mode, cleanupMode)

  fun migrateAlert(
    offenderBookId: Long = IdGenerator.newId(),
    alertCode: String = ALERT_CODE_VICTIM,
    description: String = "Alert description",
    bookingSeq: Int = 1,
    alertSeq: Int = 1,
    authorisedBy: String = "A Person",
    activeFrom: LocalDate = LocalDate.now().minusDays(2),
    activeTo: LocalDate? = null,
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(2),
    createdBy: String = "AB11DZ",
    createdByDisplayName: String = "C REATED",
    lastModifiedAt: LocalDateTime? = null,
    lastModifiedBy: String? = null,
    lastModifiedByDisplayName: String? = null,
  ) = MigrateAlert(
    offenderBookId,
    bookingSeq,
    alertSeq,
    alertCode,
    description,
    authorisedBy,
    activeFrom,
    activeTo,
    createdAt,
    createdBy,
    createdByDisplayName,
    lastModifiedAt,
    lastModifiedBy,
    lastModifiedByDisplayName,
  )
}
