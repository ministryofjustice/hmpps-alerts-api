package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response

import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert

data class PrisonersAlerts(
  val prisonNumbers: Collection<String>,
  val alerts: Collection<Alert>,
)
