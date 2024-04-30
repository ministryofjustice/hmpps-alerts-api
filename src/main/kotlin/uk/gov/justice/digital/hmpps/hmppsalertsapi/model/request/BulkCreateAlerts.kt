package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkCreateAlertMode

@Schema(
  description = "The request body for bulk creating alerts for multiple people",
)
data class BulkCreateAlerts(
  @Schema(
    description = "The prison numbers of the people to create alerts for. " +
      "Also referred to as the offender number, offender id or NOMS id.",
    example = "A1234AA",
  )
  @field:NotEmpty(message = "At least one prison number must be supplied")
  val prisonNumbers: List<String>,

  @Schema(
    description = "The alert code for the alert. A person can only have one alert using each code active at any one time. " +
      "The alert code must exist and be active.",
    example = "ABC",
  )
  @field:Size(min = 1, max = 12, message = "Alert code must be supplied and be <= 12 characters")
  val alertCode: String,

  @Schema(
    description = "The strategy to use when creating alerts in bulk",
    example = "ADD_MISSING",
  )
  val mode: BulkCreateAlertMode,

  @Schema(
    description = "The strategy to use when cleaning up existing alerts for people supplied list of prison numbers",
    example = "KEEP_ALL",
  )
  val cleanupMode: BulkCreateAlertCleanupMode,
)
