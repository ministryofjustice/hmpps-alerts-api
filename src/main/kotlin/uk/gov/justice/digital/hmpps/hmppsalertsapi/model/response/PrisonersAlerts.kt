package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert

@Schema(
  description = "Collection of alerts for a list of prisoners",
)
data class PrisonersAlerts(
  @Schema(
    description = "The distinct list of prison numbers for the prisoners with alerts in the collection.",
    example = "[\"A1234AA\", \"B2345BB\"]",
  )
  val prisonNumbers: List<String>,

  @Schema(
    description = "The list of alerts for the prisoners in the collection.",
  )
  val alerts: List<Alert>,
)
