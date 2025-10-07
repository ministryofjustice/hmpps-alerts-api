package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCodeSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AC_VICTIM
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.AT_VULNERABILITY

object RequestGenerator {
  fun AlertCode.summary() = AlertCodeSummary(alertType.code, alertType.description, code, description, true)

  fun alertCodeSummary(alertType: AlertType = AT_VULNERABILITY, alertCode: AlertCode = AC_VICTIM, canBeAdministered: Boolean = true) = AlertCodeSummary(alertType.code, alertType.description, alertCode.code, alertCode.description, canBeAdministered)
}
