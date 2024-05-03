package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class MergeAlerts(
  @Schema(
    description = "The prison number of the person the alerts are being merged from. " +
      "Also referred to as the offender number, offender id or NOMS id. " +
      "Existing alerts associated with this prison number will be deleted",
    example = "B2222BB",
  )
  @field:Size(min = 1, max = 10, message = "Prison number to merge from must be supplied and be <= 10 characters")
  val prisonNumberMergeFrom: String,

  @Schema(
    description = "The prison number of the person the alerts are being merged to. " +
      "Also referred to as the offender number, offender id or NOMS id.",
    example = "A1111AA",
  )
  @field:Size(min = 1, max = 10, message = "Prison number to merge to must be supplied and be <= 10 characters")
  val prisonNumberMergeTo: String,

  @Schema(
    description = "The alerts to add to the prison number being merged into",
  )
  @field:Valid
  val newAlerts: List<MergeAlert>,
)
