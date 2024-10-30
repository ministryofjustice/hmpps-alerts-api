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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AC_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AT_VULNERABILITY

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
}
